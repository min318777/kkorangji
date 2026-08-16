package com.min.meow.post.service;

import com.min.meow.common.PostType;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis INCR 기반 조회수 서비스
 * 흐름: 조회 요청 → Redis INCR → 스케줄러 30초마다 GETDEL로 모아 DB 배치 반영
 * 어뷰징 방지: 같은 식별자(userId or IP)는 10분 내 재조회 무시
 * 중복 집계 방지: GETDEL(원자적 읽기+삭제)로 스케줄러 중복 실행 차단
 * 블로킹 방지: KEYS 대신 SCAN으로 점진적 순회
 * 키 구조:
 *   view:count:{타입}:{ID}      → 증가분  (예: view:count:boast:123 = "45")
 *   view:lock:{타입}:{ID}:{식별자} → 어뷰징 락 (TTL 10분)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private final BoastCatPostRepository boastCatPostRepository;
    private final LostCatRepository lostCatRepository;

    private static final String COUNT_KEY_PREFIX = "view:count:";
    private static final String LOCK_KEY_PREFIX  = "view:lock:";
    private static final Duration ABUSE_LOCK_TTL = Duration.ofMinutes(10);

    /**
     * 조회수 증가 — 어뷰징 락 통과 시 Redis INCR, Redis 장애 시 DB fallback
     * @param identifier 로그인=user:{userId}, 비로그인=ip:{clientIp}
     * @return 증가 후 카운트 (어뷰징 차단 시 null)
     */
    public Long incrementViewCount(PostType postType, Long postId, String identifier) {
        String lockKey = LOCK_KEY_PREFIX + postType.name().toLowerCase() + ":" + postId + ":" + identifier;
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ABUSE_LOCK_TTL))) {
            log.debug("[조회수 어뷰징 차단] {}:{} identifier={}", postType, postId, identifier);
            return null;
        }

        // 존재하지 않는 postId는 카운트 키 자체를 만들지 않음 — 없으면 나중에 인기글 Sorted Set에
        // 좀비 데이터로 계속 쌓이고, DB UPDATE는 0건으로 조용히 무시되어 아무도 못 알아챔 (실제 발생했던 문제)
        // 락 체크 다음에 둬서, 같은 identifier의 반복 요청은 10분에 한 번만 DB 조회하도록 함
        if (!postExists(postType, postId)) {
            log.debug("[조회수] 존재하지 않는 게시글 무시 - {}:{}", postType, postId);
            return null;
        }

        String countKey = COUNT_KEY_PREFIX + postType.name().toLowerCase() + ":" + postId;
        try {
            return redisTemplate.opsForValue().increment(countKey);
        } catch (Exception e) {
            log.warn("Redis 증가 실패, DB fallback - key: {}", countKey, e);
            return incrementDbDirectly(postType, postId);
        }
    }

    private boolean postExists(PostType type, Long postId) {
        return type == PostType.BOAST
                ? boastCatPostRepository.existsById(postId)
                : lostCatRepository.existsById(postId);
    }

    /** Redis 장애 시 DB에 직접 +1 (fallback) */
    private Long incrementDbDirectly(PostType type, Long postId) {
        try {
            if (type == PostType.BOAST) boastCatPostRepository.incrementViewCount(postId);
            else if (type == PostType.LOST) lostCatRepository.incrementViewCount(postId);
            return 1L;
        } catch (Exception e) {
            log.error("DB 증가 실패 - {}:{}", type, postId, e);
            return 0L;
        }
    }

    /** Redis에 아직 반영 안 된 증가분 조회 (실제 조회수 = DB.view + 이 값) */
    public Long getViewCount(PostType postType, Long postId) {
        try {
            String value = redisTemplate.opsForValue().get(COUNT_KEY_PREFIX + postType.name().toLowerCase() + ":" + postId);
            return value != null ? Long.parseLong(value) : 0L;
        } catch (Exception e) {
            log.warn("Redis 조회 실패 - {}:{}", postType, postId, e);
            return 0L;
        }
    }

    /** DB UPDATE만 담당 — Redis 호출 없음 */
    @Transactional
    public void applyDeltasToDb(Map<String, Long> deltas) {
        int count = 0;
        for (Map.Entry<String, Long> entry : deltas.entrySet()) {
            try {
                flushDeltaToDb(entry.getKey(), entry.getValue().intValue());
                count++;
            } catch (Exception e) {
                // GETDEL 이후 DB 반영 실패 → Redis에 delta 복원 (데이터 유실 방지)
                try {
                    redisTemplate.opsForValue().increment(entry.getKey(), entry.getValue());
                    log.error("DB 반영 실패, Redis 복원 완료 - key: {}, delta: {}", entry.getKey(), entry.getValue(), e);
                } catch (Exception redisEx) {
                    log.error("DB 반영 실패 + Redis 복원도 실패 - key: {}, delta: {} (조회수 유실)",
                            entry.getKey(), entry.getValue(), redisEx);
                }
            }
        }
        log.info("조회수 동기화 완료 - {}건", count);
    }

    /**
     * Redis에서 view:count:* 키를 SCAN으로 순회하며 GETDEL로 수집
     * 단일 connection 안에서 처리해 커서 누수 방지
     */
    public Map<String, Long> collectFromRedis() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(COUNT_KEY_PREFIX + "*")
                .count(100)
                .build();

        return redisTemplate.execute((RedisCallback<Map<String, Long>>) conn -> {
            Map<String, Long> result = new HashMap<>();
            try (Cursor<byte[]> cursor = conn.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    byte[] rawKey   = cursor.next();
                    byte[] rawValue = conn.stringCommands().getDel(rawKey); // 원자적 읽기+삭제
                    if (rawValue == null) continue;
                    String key = new String(rawKey, StandardCharsets.UTF_8);
                    try {
                        long delta = Long.parseLong(new String(rawValue, StandardCharsets.UTF_8));
                        if (delta > 0) result.put(key, delta);
                    } catch (NumberFormatException e) {
                        log.warn("숫자 변환 실패 - key: {}", key);
                    }
                }
            }
            return result;
        });
    }

    /**
     * "view:count:boast:123" 키를 파싱해 DB UPDATE 실행
     * 형식 오류 시 skip
     */
    private void flushDeltaToDb(String key, int delta) {
        // view:count: 제거 후 "boast:123" 파싱
        String[] parts = key.substring(COUNT_KEY_PREFIX.length()).split(":");
        if (parts.length != 2) {
            log.warn("잘못된 키 형식 무시 - key: {}", key);
            return;
        }
        PostType type  = PostType.valueOf(parts[0].toUpperCase());
        long     postId = Long.parseLong(parts[1]);
        incrementDbByDelta(type, postId, delta);
    }

    /** DB에 delta만큼 조회수 증가 (배치 동기화용) */
    private void incrementDbByDelta(PostType type, Long postId, int delta) {
        if (type == PostType.BOAST) boastCatPostRepository.incrementViewCountByDelta(postId, delta);
        else if (type == PostType.LOST) lostCatRepository.incrementViewCountByDelta(postId, delta);
    }
}
