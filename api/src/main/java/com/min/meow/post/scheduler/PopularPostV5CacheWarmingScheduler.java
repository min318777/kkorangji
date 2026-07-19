package com.min.meow.post.scheduler;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.repository.PopularPostRepository;
import com.min.meow.post.service.PopularRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 인기글 v5 캐시 워밍 스케줄러 — Sorted Set + Cache Stampede 방지
 * 25초마다 Sorted Set에서 Top 24 ID를 가져와 post:boast:popular:v5 캐시를 선제 갱신
 * DB ORDER BY 없이 Sorted Set 기반 실시간 순위 반영
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularPostV5CacheWarmingScheduler {

    private final PopularRankingService popularRankingService;
    private final PopularPostRepository popularPostRepository;
    private final CacheManager cacheManager;

    /**
     * v5 인기글 캐시 워밍
     * 1. Sorted Set에서 Top 24 postId 조회 (DB 조회 없음)
     * 2. findByIds()로 게시글 데이터 조회
     * 3. Sorted Set score 순서로 재정렬 후 캐시 put (clear 없음 → 공백 없음)
     */
    // @Scheduled(fixedRate = 25_000, initialDelay = 0)
    public void warmV5Cache() {
        try {
            List<Long> ids = popularRankingService.getTop24PostIds();
            if (ids.isEmpty()) {
                log.debug("[v5 Cache Warming] Sorted Set 비어있음 - 워밍 생략");
                return;
            }

            List<BoastCatPostListResponse> posts = popularPostRepository.findByIds(ids);
            Map<Long, BoastCatPostListResponse> postMap = posts.stream()
                    .collect(Collectors.toMap(BoastCatPostListResponse::getId, p -> p));

            // Sorted Set score 내림차순(ids 순서) 유지
            List<BoastCatPostListResponse> fresh = ids.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Cache cache = cacheManager.getCache("post:boast:popular:v5");
            if (cache != null) {
                cache.put(SimpleKey.EMPTY, fresh);
            }
            log.info("[v5 Cache Warming] 완료 - {}개 갱신", fresh.size());
        } catch (Exception e) {
            log.warn("[v5 Cache Warming] 실패 - 다음 스케줄(25초 후) 재시도: {}", e.getMessage());
        }
    }
}
