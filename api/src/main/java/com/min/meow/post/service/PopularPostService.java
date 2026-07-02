package com.min.meow.post.service;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.repository.PopularPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 인기글 목록 조회 서비스 (v1~v5)
 * v1: @Cacheable — Stampede 방지 없음 (비교 기준선)
 * v2: Lettuce SETNX 분산 락 — Stampede 방지
 * v3: Cache Warming — Stampede 방지
 * v4: Redisson RLock — Stampede 방지
 * v5: Redis Sorted Set 실시간 집계 — DB 집계 쿼리 제거
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularPostService {

    private final PopularPostRepository popularPostRepository;
    private final PopularRankingService popularRankingService;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;

    // v1: 기본 @Cacheable — Stampede 방지 없음 (비교 기준선)
    @Cacheable(cacheNames = "post:boast:popular")
    public List<BoastCatPostListResponse> getPopularPosts() {
        log.info("[v1 Cache MISS] DB 조회 - thread: {}", Thread.currentThread().getName());
        return popularPostRepository.findTop24ByScore();
    }

    /**
     * v2: Lettuce SETNX 분산 락 — Stampede 방지
     * MISS 시 첫 스레드만 DB 조회, 나머지는 캐시 채워질 때까지 100ms 간격 대기 (최대 3초)
     */
    public List<BoastCatPostListResponse> getPopularPostsV2() {
        String lockKey = "lock:post:boast:popular";
        Cache cache = cacheManager.getCache("post:boast:popular:v2");

        Cache.ValueWrapper cached = cache.get(SimpleKey.EMPTY);
        if (cached != null) return (List<BoastCatPostListResponse>) cached.get();

        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(3));

        if (Boolean.TRUE.equals(locked)) {
            try {
                log.info("[v2 락 획득-DB 조회] thread: {}", Thread.currentThread().getName());
                List<BoastCatPostListResponse> result = popularPostRepository.findTop24ByScore();
                cache.put(SimpleKey.EMPTY, result);
                return result;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            log.info("[v2 락 대기] thread: {}", Thread.currentThread().getName());
            for (int i = 0; i < 30; i++) {
                try { Thread.sleep(100); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                Cache.ValueWrapper retry = cache.get(SimpleKey.EMPTY);
                if (retry != null) return (List<BoastCatPostListResponse>) retry.get();
            }
            log.warn("[v2 타임아웃] 안전망 DB 조회 - thread: {}", Thread.currentThread().getName());
            return popularPostRepository.findTop24ByScore();
        }
    }

    // v3: Cache Warming — PopularPostCacheWarmingScheduler가 25초마다 선제 갱신
    @Cacheable(cacheNames = "post:boast:popular:warmed")
    public List<BoastCatPostListResponse> getPopularPostsV3() {
        log.warn("[v3 Cache MISS] 스케줄러 워밍 전 DB 조회 - thread: {}", Thread.currentThread().getName());
        return popularPostRepository.findTop24ByScore();
    }

    /**
     * v4: Redisson RLock — Stampede 방지
     * v2와 동일 로직, Redisson Pub/Sub 기반 대기 + Lua 스크립트 원자적 해제
     */
    public List<BoastCatPostListResponse> getPopularPostsV4() {
        Cache cache = cacheManager.getCache("post:boast:popular:v2");
        Cache.ValueWrapper wrapper = cache.get(SimpleKey.EMPTY);
        if (wrapper != null) return (List<BoastCatPostListResponse>) wrapper.get();

        RLock lock = redissonClient.getLock("lock:post:boast:popular:v4");
        try {
            if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                try {
                    log.info("[v4 락 획득-DB 조회] thread: {}", Thread.currentThread().getName());
                    List<BoastCatPostListResponse> result = popularPostRepository.findTop24ByScore();
                    cache.put(SimpleKey.EMPTY, result);
                    return result;
                } finally {
                    lock.unlock();
                }
            }
            log.warn("[v4 타임아웃] 안전망 DB 조회 - thread: {}", Thread.currentThread().getName());
            return popularPostRepository.findTop24ByScore();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return popularPostRepository.findTop24ByScore();
        }
    }

    /**
     * v5: Redis Sorted Set 실시간 집계
     * 좋아요/댓글/조회수 변경 이벤트 → Sorted Set ZINCRBY → 목록 조회 시 ZRANGE
     * DB 집계 쿼리 없이 O(log N) 으로 TOP 24 즉시 반환
     * Sorted Set이 비어있으면 DB fallback
     */
    public List<BoastCatPostListResponse> getPopularPostsV5() {
        List<Long> ids = popularRankingService.getTop24PostIds();

        if (ids.isEmpty()) {
            log.info("[v5 Sorted Set 비어있음] DB 직접 조회 fallback");
            return popularPostRepository.findTop24ByScore();
        }

        // ID 목록으로 DB 조회 후 Sorted Set 순서(score 내림차순)로 재정렬
        List<BoastCatPostListResponse> posts = popularPostRepository.findByIds(ids);
        Map<Long, BoastCatPostListResponse> postMap = posts.stream()
                .collect(Collectors.toMap(BoastCatPostListResponse::getId, p -> p));

        return ids.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
