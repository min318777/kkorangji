package com.min.meow.post.service;

import com.min.meow.common.PostType;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.entity.BoastCatPost;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 인기글 서비스
 * [목록] v1~v5: @Cacheable / 분산 락 / Cache Warming / Redisson / Sorted Set
 * [상세] v1~v4: @Cacheable / 분산 락 / Cache Warming / 비관적 락 (스탬피드 방지 비교)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularPostService {

    private final PopularPostRepository popularPostRepository;
    private final PopularRankingService popularRankingService;
    private final ViewCountService viewCountService;
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

    // v3: Cache Warming — PopularPostCacheWarmingScheduler가 25초마다 선제 갱신 (현재 스케줄러 비활성 상태, 설계 의도만 반영)
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
     * v5: Redis Sorted Set 실시간 집계 + 캐시 워밍
     * 좋아요/댓글/조회수 이벤트 → ZINCRBY 실시간 점수 누적
     * 캐시 HIT → 즉시 반환 / MISS → Sorted Set → findByIds(DB) → 캐시 저장
     * PopularPostV5CacheWarmingScheduler가 25초마다 선제 갱신 → Stampede 방지 (현재 스케줄러 비활성 상태, 설계 의도만 반영)
     */
    @Cacheable(cacheNames = "post:boast:popular:v5")
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

    // ========== 인기글 상세조회 — Cache Stampede 방지 비교 ==========

    // v1: 기본 @Cacheable — Stampede 방지 없음 (비교 기준선)
    @Cacheable(cacheNames = "post:boast:detail", key = "#id")
    public GetBoastCatPostResponse getDetailV1(Long id) {
        log.info("[인기글 상세 v1 Cache MISS] postId: {}, thread: {}", id, Thread.currentThread().getName());
        return fetchDetail(id);
    }

    /**
     * v2: Lettuce SETNX 분산 락 — Stampede 방지
     * MISS 시 첫 스레드만 DB 조회, 나머지는 캐시 채워질 때까지 100ms 간격 대기 (최대 3초)
     */
    public GetBoastCatPostResponse getDetailV2(Long id) {
        String lockKey = "lock:post:boast:detail:" + id;
        Cache cache = cacheManager.getCache("post:boast:detail");

        Cache.ValueWrapper cached = cache.get(id);
        if (cached != null) return (GetBoastCatPostResponse) cached.get();

        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(3));

        if (Boolean.TRUE.equals(locked)) {
            try {
                Cache.ValueWrapper recheck = cache.get(id);
                if (recheck != null) return (GetBoastCatPostResponse) recheck.get();
                log.info("[인기글 상세 v2 락 획득-DB 조회] postId: {}, thread: {}", id, Thread.currentThread().getName());
                GetBoastCatPostResponse result = fetchDetail(id);
                cache.put(id, result);
                return result;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            log.info("[인기글 상세 v2 락 대기] postId: {}", id);
            for (int i = 0; i < 30; i++) {
                try { Thread.sleep(100); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                Cache.ValueWrapper retry = cache.get(id);
                if (retry != null) return (GetBoastCatPostResponse) retry.get();
            }
            log.warn("[인기글 상세 v2 타임아웃] 안전망 DB 조회 - postId: {}", id);
            return fetchDetail(id);
        }
    }

    // v3: Cache Warming — DetailCacheWarmingScheduler가 25초마다 선제 갱신 (현재 스케줄러 비활성 상태, 설계 의도만 반영)
    @Cacheable(cacheNames = "post:boast:detail", key = "#id")
    public GetBoastCatPostResponse getDetailV3(Long id) {
        log.warn("[인기글 상세 v3 Cache MISS] 워밍 실패 - postId: {}", id);
        return fetchDetail(id);
    }

    /**
     * v4: Redisson RLock — Stampede 방지
     * v2(Lettuce SETNX 100ms 폴링)와 달리 Pub/Sub 기반 대기 + Lua 스크립트 원자적 해제
     */
    public GetBoastCatPostResponse getDetailV4(Long id) {
        Cache cache = cacheManager.getCache("post:boast:detail");

        Cache.ValueWrapper cached = cache.get(id);
        if (cached != null) return (GetBoastCatPostResponse) cached.get();

        RLock lock = redissonClient.getLock("lock:post:boast:detail:v4:" + id);
        try {
            if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                try {
                    Cache.ValueWrapper recheck = cache.get(id);
                    if (recheck != null) return (GetBoastCatPostResponse) recheck.get();

                    log.info("[인기글 상세 v4 락 획득-DB 조회] postId: {}, thread: {}", id, Thread.currentThread().getName());
                    GetBoastCatPostResponse result = fetchDetail(id);
                    cache.put(id, result);
                    return result;
                } finally {
                    lock.unlock();
                }
            }
            log.warn("[인기글 상세 v4 타임아웃] 안전망 DB 조회 - postId: {}", id);
            return fetchDetail(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fetchDetail(id);
        }
    }

    /** DB 직접 조회 — 캐시 없음 (스케줄러 워밍 / 폴백용) */
    public GetBoastCatPostResponse fetchDetail(Long id) {
        BoastCatPost post = popularPostRepository.findByIdWithUser(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        long redisDelta = viewCountService.getViewCount(PostType.BOAST, id);
        return GetBoastCatPostResponse.builder()
                .id(post.getId())
                .writer(post.getUser().getNickname())
                .userId(post.getUser().getId())
                .title(post.getTitle())
                .contents(post.getContents())
                .view((int)(post.getView() + redisDelta))
                .imageUrls(new ArrayList<>(post.getImageUrls()))
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
