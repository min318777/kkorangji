package com.min.meow.post.service;

import com.min.meow.global.PostType;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redis INCR 기반 조회수 서비스 (v3 - 최적화 버전)
 *
 * 아키텍처:
 * ┌─────────────┐    INCR    ┌─────────────┐    Batch    ┌─────────────┐
 * │   Client    │ ─────────▶ │    Redis    │ ─────────▶  │   MySQL     │
 * │  (Request)  │            │   (Cache)   │  (1분마다)  │    (DB)     │
 * └─────────────┘            └─────────────┘             └─────────────┘
 *
 * 동작 방식:
 * 1. 클라이언트 요청 → Redis INCR로 조회수 증가 (즉시 반환)
 * 2. 스케줄러가 1분마다 Redis의 증가분을 DB에 배치 반영
 * 3. 동기화 후 Redis 키 삭제
 *
 * Redis 키 구조:
 * - 조회수 저장: view:count:{postType}:{postId}
 *   예: view:count:BOAST:123 = "45" (조회수 45)
 *
 * 장점:
 * - 동시성 문제 없음 (Redis INCR은 원자적)
 * - DB 부하 대폭 감소 (매 요청 대신 1분마다 배치)
 * - 초고속 응답 (메모리 연산, ~0.1ms)
 *
 * 단점 및 대응:
 * - Redis 장애 시: DB 직접 업데이트로 fallback
 * - 서버 재시작 시: 마지막 동기화 이후 1분 이내 데이터 손실 가능 (허용 범위)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViewCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private final BoastCatPostRepository boastCatPostRepository;
    private final LostCatRepository lostCatRepository;

    // Redis 키 접두사 상수
    private static final String VIEW_COUNT_KEY_PREFIX = "view:count:";

    /**
     * Redis INCR로 조회수 증가 (원자적 연산)
     *
     * Redis INCR 명령어 특징:
     * - 키가 없으면 0으로 초기화 후 1 증가
     * - 항상 원자적으로 실행 (동시 요청해도 안전)
     * - 반환값: 증가 후의 값
     *
     * 성능:
     * - 시간복잡도: O(1)
     * - 응답시간: ~0.1ms (네트워크 제외)
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     * @return 증가 후 조회수
     */
    public Long incrementViewCount(PostType postType, Long postId) {
        String key = buildKey(postType, postId);

        try {
            // Redis INCR: 키가 없으면 0에서 시작, 원자적으로 1 증가
            Long newCount = redisTemplate.opsForValue().increment(key);
            log.debug("Redis 조회수 증가 - key: {}, newCount: {}", key, newCount);
            return newCount;
        } catch (Exception e) {
            // Redis 장애 시 DB 직접 업데이트로 fallback
            log.warn("Redis 조회수 증가 실패, DB 직접 업데이트로 fallback - key: {}, error: {}",
                    key, e.getMessage());
            return incrementViewCountInDatabase(postType, postId);
        }
    }

    /**
     * 현재 조회수 조회
     *
     * Redis에서 먼저 조회하고, 없으면 0을 반환합니다.
     * (DB의 기본 조회수 + Redis 증가분이 실제 조회수)
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     * @return Redis에 저장된 조회수 증가분 (없으면 0)
     */
    public Long getViewCount(PostType postType, Long postId) {
        String key = buildKey(postType, postId);

        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0L;
        } catch (Exception e) {
            log.warn("Redis 조회수 조회 실패 - key: {}, error: {}", key, e.getMessage());
            return 0L;
        }
    }

    /**
     * 모든 Redis 조회수를 DB에 동기화 (스케줄러에서 호출)
     *
     * 동작 방식:
     * 1. view:count:* 패턴의 모든 키 스캔
     * 2. PostType별로 그룹화
     * 3. 각 게시글의 조회수를 DB에 원자적으로 반영 (view = view + delta)
     * 4. 동기화된 Redis 키 삭제
     *
     * 트랜잭션:
     * - 각 게시글별로 별도 UPDATE 쿼리 실행
     * - 하나의 실패가 전체에 영향 주지 않도록 개별 처리
     */
    @Transactional
    public void syncViewCountsToDatabase() {
        log.info("Redis → DB 조회수 동기화 시작");

        try {
            // view:count:* 패턴의 모든 키 조회
            Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");

            if (keys == null || keys.isEmpty()) {
                log.info("동기화할 조회수 데이터 없음");
                return;
            }

            // 게시글 타입별로 분류
            Map<PostType, Map<Long, Long>> viewCountsByType = new HashMap<>();
            viewCountsByType.put(PostType.BOAST, new HashMap<>());
            viewCountsByType.put(PostType.LOST, new HashMap<>());

            for (String key : keys) {
                try {
                    // 키에서 PostType과 postId 추출
                    // view:count:BOAST:123 → BOAST, 123
                    String[] parts = key.replace(VIEW_COUNT_KEY_PREFIX, "").split(":");
                    if (parts.length != 2) {
                        log.warn("잘못된 키 형식 무시 - key: {}", key);
                        continue;
                    }

                    PostType postType = PostType.valueOf(parts[0]);
                    Long postId = Long.parseLong(parts[1]);
                    String value = redisTemplate.opsForValue().get(key);

                    if (value != null) {
                        Long delta = Long.parseLong(value);
                        viewCountsByType.get(postType).put(postId, delta);
                    }
                } catch (Exception e) {
                    log.warn("키 파싱 실패, 건너뜀 - key: {}, error: {}", key, e.getMessage());
                }
            }

            // DB 업데이트
            int boastUpdated = syncBoastCatPosts(viewCountsByType.get(PostType.BOAST));
            int lostUpdated = syncLostCatPosts(viewCountsByType.get(PostType.LOST));

            // 동기화된 키 삭제
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            log.info("Redis → DB 조회수 동기화 완료 - BOAST: {}건, LOST: {}건",
                    boastUpdated, lostUpdated);

        } catch (Exception e) {
            log.error("조회수 동기화 중 오류 발생", e);
            // 다음 스케줄에서 재시도
        }
    }

    /**
     * 특정 게시글의 Redis 조회수를 DB에 동기화
     *
     * 개별 동기화가 필요한 경우 (예: 게시글 삭제 전) 사용합니다.
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     */
    @Transactional
    public void syncViewCountToDatabase(PostType postType, Long postId) {
        String key = buildKey(postType, postId);

        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return;
            }

            Long delta = Long.parseLong(value);
            if (delta > 0) {
                updateViewCountInDatabase(postType, postId, delta);
                redisTemplate.delete(key);
                log.debug("개별 조회수 동기화 완료 - postType: {}, postId: {}, delta: {}",
                        postType, postId, delta);
            }
        } catch (Exception e) {
            log.warn("개별 조회수 동기화 실패 - postType: {}, postId: {}, error: {}",
                    postType, postId, e.getMessage());
        }
    }

    /**
     * Redis 키 생성
     *
     * @param postType 게시글 타입
     * @param postId 게시글 ID
     * @return Redis 키 (예: view:count:BOAST:123)
     */
    private String buildKey(PostType postType, Long postId) {
        return VIEW_COUNT_KEY_PREFIX + postType.name() + ":" + postId;
    }

    /**
     * Redis 장애 시 DB 직접 업데이트 (fallback)
     */
    private Long incrementViewCountInDatabase(PostType postType, Long postId) {
        try {
            if (postType == PostType.BOAST) {
                boastCatPostRepository.incrementViewCount(postId);
            } else if (postType == PostType.LOST) {
                lostCatRepository.incrementViewCount(postId);
            }
            // DB의 경우 증가 후 값을 반환하기 어려우므로 1을 반환
            return 1L;
        } catch (Exception e) {
            log.error("DB 조회수 증가 실패 - postType: {}, postId: {}", postType, postId, e);
            return 0L;
        }
    }

    /**
     * DB에 조회수 증가분 반영 (원자적 UPDATE)
     */
    private void updateViewCountInDatabase(PostType postType, Long postId, Long delta) {
        if (postType == PostType.BOAST) {
            boastCatPostRepository.incrementViewCountByDelta(postId, delta.intValue());
        } else if (postType == PostType.LOST) {
            lostCatRepository.incrementViewCountByDelta(postId, delta.intValue());
        }
    }

    /**
     * BoastCatPost 조회수 일괄 동기화
     */
    private int syncBoastCatPosts(Map<Long, Long> viewCounts) {
        int updated = 0;
        for (Map.Entry<Long, Long> entry : viewCounts.entrySet()) {
            try {
                boastCatPostRepository.incrementViewCountByDelta(
                        entry.getKey(),
                        entry.getValue().intValue()
                );
                updated++;
            } catch (Exception e) {
                log.warn("BoastCatPost 조회수 동기화 실패 - postId: {}, error: {}",
                        entry.getKey(), e.getMessage());
            }
        }
        return updated;
    }

    /**
     * LostCatPost 조회수 일괄 동기화
     */
    private int syncLostCatPosts(Map<Long, Long> viewCounts) {
        int updated = 0;
        for (Map.Entry<Long, Long> entry : viewCounts.entrySet()) {
            try {
                lostCatRepository.incrementViewCountByDelta(
                        entry.getKey(),
                        entry.getValue().intValue()
                );
                updated++;
            } catch (Exception e) {
                log.warn("LostCatPost 조회수 동기화 실패 - postId: {}, error: {}",
                        entry.getKey(), e.getMessage());
            }
        }
        return updated;
    }
}
