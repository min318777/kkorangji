package com.min.meow.post.scheduler;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.repository.PopularPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 인기 게시물 캐시 워밍 스케줄러 — Cache Stampede 방지
 * 현재 @Scheduled 주석 처리로 비활성 상태 (v1 무방지 방식과의 비교 기준선 유지 목적).
 * 활성화 시 TTL(30초) 만료 전 선제 갱신하여 MISS를 방지하는 것이 설계 의도.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularPostCacheWarmingScheduler {

    private final CacheManager cacheManager;
    private final PopularPostRepository popularPostRepository;

    /**
     * 인기 게시물(v3) 캐시 워밍 — Cache Stampede 방지
     * - Before: cache.clear() → DB 조회 → put() → clear와 put 사이 공백 → Stampede 재발
     * - After:  DB 먼저 조회 → 즉시 put() 덮어쓰기 → 공백 없음
     * DB 조회 중에는 기존 캐시 데이터가 그대로 유지되므로
     * 갱신 도중 들어오는 요청도 항상 HIT 보장
     * fixedRate = 25_000ms = 25초 (TTL 30초보다 5초 앞서 갱신)
     */
    //@Scheduled(fixedRate = 25_000, initialDelay = 0)
    public void warmPopularPostsCache() {
        log.info("[v3 Cache Warming] 인기 게시물 캐시 갱신 시작");
        try {
            // 1. DB 먼저 조회 (이 동안 기존 캐시 유지 → 요청은 계속 HIT)
            List<BoastCatPostListResponse> fresh = popularPostRepository.findTop24ByScore();

            // 2. 즉시 덮어쓰기 (clear 없음 → 공백 없음)
            Cache cache = cacheManager.getCache("post:boast:popular:warmed");
            if (cache != null) {
                cache.put(SimpleKey.EMPTY, fresh);
            }
            log.info("[v3 Cache Warming] 인기 게시물 캐시 갱신 완료");
        } catch (Exception e) {
            log.warn("[v3 Cache Warming] 실패 - 다음 스케줄(25초 후) 재시도: {}", e.getMessage());
        }
    }
}
