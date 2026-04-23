package com.min.meow.post.postlike.service;

import com.min.meow.global.PostType;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.postlike.entity.PostLike;
import com.min.meow.postlike.repository.PostLikeRepository;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * 좋아요 서비스 (Redis + @Async DB 기록 방식)
 *
 * 아키텍처:
 * ┌─────────────┐  SADD/SREM  ┌─────────────┐  @Async  ┌─────────────┐
 * │   Client    │ ──────────▶ │    Redis    │ ───────▶  │   MySQL     │
 * │  (Request)  │   ~1ms 응답 │  (SET)      │ (즉시,    │ post_like   │
 * └─────────────┘             └─────────────┘  비동기)  └─────────────┘
 *
 * 핵심 원칙:
 * - Redis: 속도 (즉각 응답, 중복 방지)
 * - DB: 진실의 원천 (영속 저장, Redis 초기화 시 복구 기준)
 * - @Async: Kafka 없이 비동기 DB 기록
 *
 * Redis 키:
 * - like:users:{postType}:{postId} → SET {userId1, userId2, ...}
 *
 * Redis 장애 시:
 * - DB에 직접 처리 (fallback) → 응답 느리지만 데이터 손실 없음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PostLikeRepository postLikeRepository;
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;
    private final LikeAsyncWriter likeAsyncWriter;

    private static final String LIKE_USERS_KEY_PREFIX = "like:users:";

    /**
     * 좋아요 토글
     *
     * 1. Redis SET에서 중복 체크 + 즉각 응답
     * 2. @Async로 DB 비동기 기록 (PostLike INSERT/DELETE + likeCount 원자적 UPDATE)
     *
     * @return true: 좋아요 등록됨, false: 좋아요 취소됨
     */
    public boolean toggleLike(PostType postType, Long postId, Long userId) {
        String key = buildKey(postType, postId);
        String userIdStr = userId.toString();

        try {
            SetOperations<String, String> setOps = redisTemplate.opsForSet();

            // 캐시 미스 시 DB에서 복구 (Cache Aside)
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                loadFromDb(postType, postId, key, setOps);
            }

            Boolean isMember = setOps.isMember(key, userIdStr);
            boolean liked;

            if (Boolean.TRUE.equals(isMember)) {
                setOps.remove(key, userIdStr);
                liked = false;
            } else {
                setOps.add(key, userIdStr);
                liked = true;
            }

            // DB 비동기 기록 (LikeAsyncWriter는 별도 빈이어야 @Async 동작)
            likeAsyncWriter.persist(postId, userId, liked);

            return liked;

        } catch (Exception e) {
            log.error("Redis 좋아요 토글 실패, DB fallback 전환 - postId: {}, userId: {}, error: {}",
                    postId, userId, e.getMessage());
            return toggleLikeInDatabase(postId, userId);
        }
    }

    /**
     * 좋아요 여부 확인 (Redis SISMEMBER, O(1))
     */
    public boolean isLiked(PostType postType, Long postId, Long userId) {
        String key = buildKey(postType, postId);

        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                loadFromDb(postType, postId, key, redisTemplate.opsForSet());
            }
            Boolean isMember = redisTemplate.opsForSet().isMember(key, userId.toString());
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.warn("Redis 좋아요 여부 확인 실패, DB fallback - error: {}", e.getMessage());
            return postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId);
        }
    }

    /**
     * 좋아요 수 조회 (Redis SCARD, O(1))
     */
    public Long getLikeCount(PostType postType, Long postId) {
        String key = buildKey(postType, postId);

        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                loadFromDb(postType, postId, key, redisTemplate.opsForSet());
            }
            Long count = redisTemplate.opsForSet().size(key);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Redis 좋아요 수 조회 실패, DB fallback - error: {}", e.getMessage());
            return postLikeRepository.countByBoastCatPostId(postId);
        }
    }

    // ==================== Private Methods ====================

    /**
     * DB에서 Redis SET 복구 (Cache Aside 패턴)
     *
     * 좋아요가 없는 게시글(빈 결과)은 Redis에 키를 생성하지 않습니다.
     * → 첫 좋아요가 들어오면 SADD로 자동 생성됩니다.
     * → 좋아요 0인 게시글은 항상 DB를 조회하지만 부하가 낮으므로 단순함을 선택합니다.
     */
    private void loadFromDb(PostType postType, Long postId,
                            String key, SetOperations<String, String> setOps) {
        if (postType != PostType.BOAST) return;

        try {
            Set<Long> userIds = postLikeRepository.findUserIdsByBoastCatPostId(postId);
            if (!userIds.isEmpty()) {
                String[] arr = userIds.stream().map(String::valueOf).toArray(String[]::new);
                setOps.add(key, arr);
                log.debug("Redis 캐시 복구 완료 - postId: {}, userCount: {}", postId, userIds.size());
            }
            // 빈 게시글은 키 생성 안 함 → 첫 좋아요 시 SADD로 자동 생성됨
        } catch (Exception e) {
            log.warn("Redis 캐시 복구 실패 - postId: {}, error: {}", postId, e.getMessage());
        }
    }

    /**
     * Redis 장애 시 DB 직접 처리 (fallback)
     *
     * 응답은 느리지만 데이터 손실 없이 처리합니다.
     * DB UniqueConstraint가 최종 안전망 역할을 합니다.
     */
    @Transactional
    public boolean toggleLikeInDatabase(Long postId, Long userId) {
        Optional<PostLike> existing = postLikeRepository.findByBoastCatPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            boastCatPostRepository.incrementLikeCountByDelta(postId, -1);
            return false;
        }

        BoastCatPost post = boastCatPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        PostLike like = PostLike.builder()
                .user(userRepository.getReferenceById(userId))
                .boastCatPost(post)
                .build();
        postLikeRepository.save(like);
        boastCatPostRepository.incrementLikeCountByDelta(postId, 1);
        return true;
    }

    private String buildKey(PostType postType, Long postId) {
        return LIKE_USERS_KEY_PREFIX + postType.name() + ":" + postId;
    }
}
