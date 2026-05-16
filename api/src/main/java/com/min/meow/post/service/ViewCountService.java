package com.min.meow.post.service;

import com.min.meow.global.PostType;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis INCR 기반 조회수 서비스
 *
 * 아키텍처:
 * ┌─────────────┐    INCR     ┌─────────────┐   SCAN+GETDEL   ┌─────────────┐
 * │   Client    │ ──────────▶ │    Redis    │ ──────────────▶  │   MySQL     │
 * │  (Request)  │             │   (Cache)   │    (1분마다)     │    (DB)     │
 * └─────────────┘             └─────────────┘                  └─────────────┘
 *
 * Race Condition 해결 (핵심):
 * - 기존: GET(읽기) → DELETE(삭제) 사이 새 INCR이 오면 해당 값 영구 손실
 * - 개선: GETDEL(원자적 읽기+삭제) → GET과 DELETE 사이 간격 자체가 없음
 *
 * Redis 블로킹 해결:
 * - 기존: KEYS 명령어 → Redis 단일스레드 특성상 전체 블로킹 (프로덕션 금지)
 * - 개선: SCAN 명령어 → 논블로킹, 커서 방식으로 점진적 조회
 *
 * Redis 키 구조:
 * - view:count:{postType}:{postId} = "증가분"
 *   예: view:count:BOAST:123 = "45" (DB에 아직 반영 안 된 증가분 45)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViewCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private final BoastCatPostRepository boastCatPostRepository;
    private final LostCatRepository lostCatRepository;

    private static final String VIEW_COUNT_KEY_PREFIX = "view:count:";
    private static final String VIEW_LOCK_KEY_PREFIX = "view:lock:";

    /**
     * Redis INCR로 조회수 증가 — 어뷰징 방지 포함
     *
     * 어뷰징 방지: setIfAbsent로 10분 분산 락 획득
     * - 락 획득 성공(true) → 첫 조회 → 카운트 증가
     * - 락 획득 실패(false) → 10분 내 재조회 → 스킵
     *
     * identifier: 로그인=user:{userId}, 비로그인=ip:{clientIp}
     */
    public Long incrementViewCount(PostType postType, Long postId, String identifier) {
        // 어뷰징 방지 락 키: view:lock:BOAST:123:user:42
        String lockKey = VIEW_LOCK_KEY_PREFIX + postType.name() + ":" + postId + ":" + identifier;
        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(10));

        if (!Boolean.TRUE.equals(isFirst)) {
            log.debug("[조회수 어뷰징 차단] key: {}", lockKey);
            return null;
        }

        String countKey = buildKey(postType, postId);
        try {
            Long newCount = redisTemplate.opsForValue().increment(countKey);
            log.debug("Redis 조회수 증가 - key: {}, newCount: {}", countKey, newCount);
            return newCount;
        } catch (Exception e) {
            // Redis 장애 시 DB 원자적 UPDATE로 fallback
            log.warn("Redis 조회수 증가 실패, DB fallback - key: {}, error: {}", countKey, e.getMessage());
            return incrementViewCountInDatabase(postType, postId);
        }
    }

    /**
     * Redis에 저장된 조회수 증가분 조회 (DB 기본값 미포함)
     * - 실제 조회수 = DB의 view 컬럼 값 + 이 메서드 반환값
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
     * SCAN + GETDEL 전략:
     * 1. SCAN으로 view:count:* 키를 논블로킹 순회 (KEYS 대신)
     * 2. 각 키에 GETDEL → 원자적으로 값 읽고 즉시 삭제
     *    → GETDEL과 GETDEL 사이에 들어온 INCR은 다음 스케줄에 반영
     *    → GETDEL 내부에서 중간에 끼어들 수 없으므로 데이터 손실 없음
     * 3. 수집된 delta를 DB에 배치 반영
     */
    @Transactional
    public void syncViewCountsToDatabase() {
        log.info("Redis → DB 조회수 동기화 시작");

        try {
            Map<PostType, Map<Long, Long>> viewCountsByType = new HashMap<>();
            viewCountsByType.put(PostType.BOAST, new HashMap<>());
            viewCountsByType.put(PostType.LOST, new HashMap<>());

            // SCAN: KEYS와 달리 Redis를 블로킹하지 않음
            // count(100)은 힌트값 (실제 반환 개수는 Redis가 결정)
            ScanOptions options = ScanOptions.scanOptions()
                    .match(VIEW_COUNT_KEY_PREFIX + "*")
                    .count(100)
                    .build();

            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    try {
                        // GETDEL: GET + DEL 원자적 실행 → Race Condition 원천 차단
                        // GETDEL 이후 해당 키에 새 INCR이 오면 새 키로 재생성 → 다음 스케줄에 반영
                        String value = redisTemplate.opsForValue().getAndDelete(key);
                        if (value == null) continue;

                        long delta = Long.parseLong(value);
                        if (delta <= 0) continue;

                        // "view:count:BOAST:123" → ["BOAST", "123"]
                        String suffix = key.substring(VIEW_COUNT_KEY_PREFIX.length());
                        String[] parts = suffix.split(":");
                        if (parts.length != 2) {
                            log.warn("잘못된 키 형식 무시 - key: {}", key);
                            continue;
                        }

                        PostType postType = PostType.valueOf(parts[0]);
                        Long postId = Long.parseLong(parts[1]);
                        viewCountsByType.get(postType).put(postId, delta);

                    } catch (Exception e) {
                        log.warn("키 처리 실패, 건너뜀 - key: {}, error: {}", key, e.getMessage());
                    }
                }
            }

            // DB 배치 업데이트
            int boastUpdated = syncBoastCatPosts(viewCountsByType.get(PostType.BOAST));
            int lostUpdated = syncLostCatPosts(viewCountsByType.get(PostType.LOST));

            log.info("Redis → DB 조회수 동기화 완료 - BOAST: {}건, LOST: {}건", boastUpdated, lostUpdated);

        } catch (Exception e) {
            log.error("조회수 동기화 중 오류 발생", e);
        }
    }

    /**
     * 특정 게시글 조회수 개별 동기화 (게시글 삭제 전 호출용)
     * GETDEL로 원자적 처리
     */
    @Transactional
    public void syncViewCountToDatabase(PostType postType, Long postId) {
        String key = buildKey(postType, postId);
        try {
            // GETDEL: 읽고 즉시 삭제 (원자적)
            String value = redisTemplate.opsForValue().getAndDelete(key);
            if (value == null) return;

            long delta = Long.parseLong(value);
            if (delta > 0) {
                updateViewCountInDatabase(postType, postId, delta);
                log.debug("개별 조회수 동기화 완료 - postType: {}, postId: {}, delta: {}", postType, postId, delta);
            }
        } catch (Exception e) {
            log.warn("개별 조회수 동기화 실패 - postType: {}, postId: {}, error: {}", postType, postId, e.getMessage());
        }
    }

    private String buildKey(PostType postType, Long postId) {
        return VIEW_COUNT_KEY_PREFIX + postType.name() + ":" + postId;
    }

    private Long incrementViewCountInDatabase(PostType postType, Long postId) {
        try {
            if (postType == PostType.BOAST) {
                boastCatPostRepository.incrementViewCount(postId);
            } else if (postType == PostType.LOST) {
                lostCatRepository.incrementViewCount(postId);
            }
            return 1L;
        } catch (Exception e) {
            log.error("DB 조회수 증가 실패 - postType: {}, postId: {}", postType, postId, e);
            return 0L;
        }
    }

    private void updateViewCountInDatabase(PostType postType, Long postId, long delta) {
        if (postType == PostType.BOAST) {
            boastCatPostRepository.incrementViewCountByDelta(postId, (int) delta);
        } else if (postType == PostType.LOST) {
            lostCatRepository.incrementViewCountByDelta(postId, (int) delta);
        }
    }

    private int syncBoastCatPosts(Map<Long, Long> viewCounts) {
        int updated = 0;
        for (Map.Entry<Long, Long> entry : viewCounts.entrySet()) {
            try {
                boastCatPostRepository.incrementViewCountByDelta(entry.getKey(), entry.getValue().intValue());
                updated++;
            } catch (Exception e) {
                log.warn("BoastCatPost 조회수 동기화 실패 - postId: {}, error: {}", entry.getKey(), e.getMessage());
            }
        }
        return updated;
    }

    private int syncLostCatPosts(Map<Long, Long> viewCounts) {
        int updated = 0;
        for (Map.Entry<Long, Long> entry : viewCounts.entrySet()) {
            try {
                lostCatRepository.incrementViewCountByDelta(entry.getKey(), entry.getValue().intValue());
                updated++;
            } catch (Exception e) {
                log.warn("LostCatPost 조회수 동기화 실패 - postId: {}, error: {}", entry.getKey(), e.getMessage());
            }
        }
        return updated;
    }
}
