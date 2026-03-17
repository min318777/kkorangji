package com.min.meow.post.scheduler;

import com.min.meow.post.service.BoastCatPostService;
import com.min.meow.post.service.LostCatPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 최근 게시글 캐시 워밍(Cache Warming) 스케줄러
 * 목적: Cache Stampede(캐시 스탬피드) 방지
 * 문제 상황:
 * - post:boast:recent, post:lost:recent는 모든 사용자가 공유하는 단 1개의 키
 * - TTL 5분 만료 순간 수많은 요청이 동시에 Cache Miss → DB 폭격
 * 해결 방법:
 * - TTL 만료 전(4분 50초마다)에 스케줄러가 미리 캐시를 갱신
 * - Cache Miss 자체가 발생하지 않도록 예방
 * 동작 방식:
 * 1. 기존 캐시 삭제 (evict)
 * 2. 서비스 메서드 직접 호출 → @Cacheable이 자동으로 새 데이터 적재
 * 3. 이후 요청은 항상 Cache HIT
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecentPostCacheWarmingScheduler {

    private final BoastCatPostService boastCatPostService;
    private final LostCatPostService lostCatPostService;
    private final CacheManager cacheManager;

    /**
     * 최근 자랑글 + 실종글 캐시 워밍
     *
     * fixedRate = 290_000ms = 4분 50초
     * → TTL 5분보다 10초 앞서 갱신하여 만료 구간 제거
     *
     * initialDelay = 0
     * → 애플리케이션 시작 시 즉시 1회 실행 (콜드 스타트 대응)
     */
    @Scheduled(fixedRate = 290_000, initialDelay = 0)
    public void warmRecentPostsCache() {
        log.info("최근 게시글 캐시 워밍 시작");

        warmBoastCatRecentCache();
        warmLostCatRecentCache();

        log.info("최근 게시글 캐시 워밍 완료");
    }

    /**
     * 자랑글 최근 목록 캐시 갱신
     * 1. 기존 캐시 삭제
     * 2. 서비스 호출 → @Cacheable이 자동으로 새 데이터를 Redis에 적재
     */
    private void warmBoastCatRecentCache() {
        try {
            // 기존 캐시 삭제 (만료 전 강제 제거)
            var cache = cacheManager.getCache("post:boast:recent");
            if (cache != null) {
                cache.clear();
            }
            // @Cacheable → Cache Miss 유발 → DB 조회 후 자동 적재
            boastCatPostService.getRecentBoastCatPosts();
            log.debug("자랑글 최근 목록 캐시 워밍 완료");
        } catch (Exception e) {
            // 워밍 실패해도 서비스 중단 없이 다음 스케줄에서 재시도
            log.warn("자랑글 최근 목록 캐시 워밍 실패 - 다음 스케줄에서 재시도: {}", e.getMessage());
        }
    }

    /**
     * 실종글 최근 목록 캐시 갱신
     */
    private void warmLostCatRecentCache() {
        try {
            var cache = cacheManager.getCache("post:lost:recent");
            if (cache != null) {
                cache.clear();
            }
            lostCatPostService.getRecentLostCatPosts();
            log.debug("실종글 최근 목록 캐시 워밍 완료");
        } catch (Exception e) {
            log.warn("실종글 최근 목록 캐시 워밍 실패 - 다음 스케줄에서 재시도: {}", e.getMessage());
        }
    }
}