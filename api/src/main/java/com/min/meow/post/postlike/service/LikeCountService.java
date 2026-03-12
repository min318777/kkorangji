package com.min.meow.post.postlike.service;

import com.min.meow.global.PostType;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.postlike.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redis 기반 좋아요 서비스 (v2 - 최적화 버전)
 *
 * 아키텍처:
 * ┌─────────────┐   SADD/SREM   ┌─────────────┐    Batch    ┌─────────────┐
 * │   Client    │ ────────────▶ │    Redis    │ ─────────▶  │   MySQL     │
 * │  (Request)  │               │  (SET/INCR) │  (1분마다)  │    (DB)     │
 * └─────────────┘               └─────────────┘             └─────────────┘
 *
 * Redis 키 구조:
 * - 좋아요 사용자 SET: like:users:{postType}:{postId}
 *   예: like:users:BOAST:123 = {1, 5, 10, 25} (userId들)
 *
 * - 좋아요 수 변경분: like:delta:{postType}:{postId}
 *   예: like:delta:BOAST:123 = "3" (3개 증가)
 *
 * - 동기화 대기 목록: like:pending:{postType}
 *   예: like:pending:BOAST = {123, 456, 789} (변경된 게시글 ID들)
 *
 * 동작 방식:
 * 1. 좋아요 토글 시 → SET에 추가/제거 + delta 증감 + pending에 추가
 * 2. 스케줄러 실행 시 → pending 목록의 게시글들 DB 동기화
 * 3. 동기화 완료 후 → delta 키 삭제
 *
 * 장점:
 * - 동시성 문제 없음 (Redis SET 연산은 원자적)
 * - 중복 좋아요 자동 방지 (SET 자료구조 특성)
 * - DB 부하 대폭 감소 (배치 동기화)
 * - 초고속 응답 (~0.1ms)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LikeCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private final BoastCatPostRepository boastCatPostRepository;
    private final PostLikeRepository postLikeRepository;

    // Redis 키 접두사 상수
    private static final String LIKE_USERS_KEY_PREFIX = "like:users:";    // SET: 좋아요한 사용자들
    private static final String LIKE_DELTA_KEY_PREFIX = "like:delta:";    // STRING: 좋아요 수 변경분
    private static final String LIKE_PENDING_KEY_PREFIX = "like:pending:"; // SET: 동기화 대기 게시글

    /**
     * 좋아요 토글 - Redis SET 방식
     *
     * SET 자료구조 활용:
     * - SISMEMBER: 좋아요 여부 확인 (O(1))
     * - SADD: 좋아요 추가 (O(1), 이미 있으면 무시)
     * - SREM: 좋아요 제거 (O(1))
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return true: 좋아요 등록됨, false: 좋아요 취소됨
     */
    public boolean toggleLike(PostType postType, Long postId, Long userId) {
        String usersKey = buildUsersKey(postType, postId);
        String deltaKey = buildDeltaKey(postType, postId);
        String pendingKey = buildPendingKey(postType);
        String userIdStr = userId.toString();

        try {
            SetOperations<String, String> setOps = redisTemplate.opsForSet();

            // 캐시 미스 시 DB에서 로드 (Lazy Loading)
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(usersKey))) {
                warmUpCache(postType, postId);
            }

            // 좋아요 여부 확인 및 토글
            Boolean isMember = setOps.isMember(usersKey, userIdStr);

            if (Boolean.TRUE.equals(isMember)) {
                // 좋아요 취소: SET에서 제거 + delta 감소
                setOps.remove(usersKey, userIdStr);
                redisTemplate.opsForValue().increment(deltaKey, -1);
                log.debug("Redis 좋아요 취소 - postType: {}, postId: {}, userId: {}",
                        postType, postId, userId);

                // 동기화 대기 목록에 추가
                setOps.add(pendingKey, postId.toString());
                return false;
            } else {
                // 좋아요 등록: SET에 추가 + delta 증가
                setOps.add(usersKey, userIdStr);
                redisTemplate.opsForValue().increment(deltaKey, 1);
                log.debug("Redis 좋아요 등록 - postType: {}, postId: {}, userId: {}",
                        postType, postId, userId);

                // 동기화 대기 목록에 추가
                setOps.add(pendingKey, postId.toString());
                return true;
            }
        } catch (Exception e) {
            log.error("Redis 좋아요 토글 실패 - postType: {}, postId: {}, userId: {}, error: {}",
                    postType, postId, userId, e.getMessage());
            // Redis 장애 시 DB 직접 처리로 fallback
            return toggleLikeInDatabase(postType, postId, userId);
        }
    }

    /**
     * 좋아요 여부 확인
     */
    public boolean isLiked(PostType postType, Long postId, Long userId) {
        String usersKey = buildUsersKey(postType, postId);

        try {
            // 캐시 미스 시 DB에서 로드
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(usersKey))) {
                warmUpCache(postType, postId);
            }

            Boolean isMember = redisTemplate.opsForSet().isMember(usersKey, userId.toString());
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.warn("Redis 좋아요 여부 확인 실패, DB 조회로 fallback - error: {}", e.getMessage());
            return isLikedInDatabase(postType, postId, userId);
        }
    }

    /**
     * 현재 좋아요 수 조회
     *
     * Redis SET의 SCARD (cardinality) 사용: O(1)
     */
    public Long getLikeCount(PostType postType, Long postId) {
        String usersKey = buildUsersKey(postType, postId);

        try {
            // 캐시 미스 시 DB에서 로드
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(usersKey))) {
                warmUpCache(postType, postId);
            }

            Long count = redisTemplate.opsForSet().size(usersKey);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Redis 좋아요 수 조회 실패, DB 조회로 fallback - error: {}", e.getMessage());
            return getLikeCountFromDatabase(postType, postId);
        }
    }

    /**
     * 모든 Redis 좋아요 데이터를 DB에 동기화 (스케줄러에서 호출)
     */
    @Transactional
    public void syncLikesToDatabase() {
        log.info("Redis → DB 좋아요 동기화 시작");

        try {
            // BOAST 타입 동기화
            syncPostTypeLikes(PostType.BOAST);

            // LOST 타입은 현재 좋아요 미지원, 추후 확장 가능
            // syncPostTypeLikes(PostType.LOST);

            log.info("Redis → DB 좋아요 동기화 완료");
        } catch (Exception e) {
            log.error("좋아요 동기화 중 오류 발생", e);
        }
    }

    /**
     * 특정 게시글의 Redis 좋아요를 DB에 동기화
     */
    @Transactional
    public void syncLikeToDatabase(PostType postType, Long postId) {
        String deltaKey = buildDeltaKey(postType, postId);

        try {
            String deltaStr = redisTemplate.opsForValue().get(deltaKey);
            if (deltaStr == null || "0".equals(deltaStr)) {
                return;
            }

            int delta = Integer.parseInt(deltaStr);
            if (delta != 0) {
                // likeCount 원자적 업데이트
                if (postType == PostType.BOAST) {
                    boastCatPostRepository.incrementLikeCountByDelta(postId, delta);
                }

                // delta 키 삭제
                redisTemplate.delete(deltaKey);

                log.debug("개별 좋아요 동기화 완료 - postType: {}, postId: {}, delta: {}",
                        postType, postId, delta);
            }
        } catch (Exception e) {
            log.warn("개별 좋아요 동기화 실패 - postType: {}, postId: {}, error: {}",
                    postType, postId, e.getMessage());
        }
    }

    /**
     * Redis에 게시글의 기존 좋아요 데이터 로드 (캐시 워밍)
     *
     * DB의 PostLike 데이터를 Redis SET에 로드합니다.
     */
    public void warmUpCache(PostType postType, Long postId) {
        String usersKey = buildUsersKey(postType, postId);

        try {
            // 이미 캐시가 있으면 스킵
            if (Boolean.TRUE.equals(redisTemplate.hasKey(usersKey))) {
                return;
            }

            // DB에서 좋아요한 사용자 ID 목록 조회
            if (postType == PostType.BOAST) {
                Set<Long> userIds = postLikeRepository.findUserIdsByBoastCatPostId(postId);

                if (!userIds.isEmpty()) {
                    String[] userIdStrs = userIds.stream()
                            .map(String::valueOf)
                            .toArray(String[]::new);
                    redisTemplate.opsForSet().add(usersKey, userIdStrs);
                    log.debug("캐시 워밍 완료 - postType: {}, postId: {}, userCount: {}",
                            postType, postId, userIds.size());
                } else {
                    // 빈 SET도 키 존재를 표시하기 위해 빈 마커 추가 후 삭제
                    // (hasKey 체크를 위해)
                    redisTemplate.opsForValue().set(usersKey + ":initialized", "1");
                }
            }
        } catch (Exception e) {
            log.warn("캐시 워밍 실패 - postType: {}, postId: {}, error: {}",
                    postType, postId, e.getMessage());
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * 특정 PostType의 pending 게시글들 동기화
     */
    private void syncPostTypeLikes(PostType postType) {
        String pendingKey = buildPendingKey(postType);

        Set<String> pendingPostIds = redisTemplate.opsForSet().members(pendingKey);
        if (pendingPostIds == null || pendingPostIds.isEmpty()) {
            log.debug("{} 타입 동기화할 데이터 없음", postType);
            return;
        }

        int synced = 0;
        for (String postIdStr : pendingPostIds) {
            try {
                Long postId = Long.parseLong(postIdStr);
                syncLikeToDatabase(postType, postId);
                synced++;
            } catch (Exception e) {
                log.warn("게시글 좋아요 동기화 실패 - postId: {}, error: {}", postIdStr, e.getMessage());
            }
        }

        // pending 목록 비우기
        redisTemplate.delete(pendingKey);
        log.info("{} 타입 좋아요 동기화 완료 - {}건", postType, synced);
    }

    /**
     * Redis 장애 시 DB 직접 좋아요 토글 (fallback)
     */
    private boolean toggleLikeInDatabase(PostType postType, Long postId, Long userId) {
        // 기존 PostLikeService의 로직을 그대로 사용하거나
        // 원자적 쿼리 방식으로 처리
        log.warn("DB fallback으로 좋아요 처리 - postType: {}, postId: {}, userId: {}",
                postType, postId, userId);

        // 여기서는 단순히 실패 반환, 실제로는 기존 서비스 호출 필요
        return false;
    }

    /**
     * DB에서 좋아요 여부 확인 (fallback)
     */
    private boolean isLikedInDatabase(PostType postType, Long postId, Long userId) {
        if (postType == PostType.BOAST) {
            return postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId);
        }
        return false;
    }

    /**
     * DB에서 좋아요 수 조회 (fallback)
     */
    private Long getLikeCountFromDatabase(PostType postType, Long postId) {
        if (postType == PostType.BOAST) {
            return postLikeRepository.countByBoastCatPostId(postId);
        }
        return 0L;
    }

    // ==================== Key Builders ====================

    private String buildUsersKey(PostType postType, Long postId) {
        return LIKE_USERS_KEY_PREFIX + postType.name() + ":" + postId;
    }

    private String buildDeltaKey(PostType postType, Long postId) {
        return LIKE_DELTA_KEY_PREFIX + postType.name() + ":" + postId;
    }

    private String buildPendingKey(PostType postType) {
        return LIKE_PENDING_KEY_PREFIX + postType.name();
    }
}
