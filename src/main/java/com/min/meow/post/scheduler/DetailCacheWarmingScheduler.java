package com.min.meow.post.scheduler;

import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.service.PopularPostService;
import com.min.meow.post.service.PopularRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetailCacheWarmingScheduler {

    private final PopularRankingService popularRankingService;
    private final PopularPostService popularPostService;
    private final CacheManager cacheManager;

    // 인기글 상세 캐시 워밍(25초마다) — Sorted Set TOP 24 조회 후 캐시 put (clear 없이 덮어써서 갱신 중 기존 데이터 유지)
    // @Scheduled(fixedRate = 25_000, initialDelay = 5_000) // v1 Stampede 테스트 시 비활성화
    public void warmDetailCaches() {
        List<Long> topIds = popularRankingService.getTop24PostIds();
        if (topIds.isEmpty()) {
            log.debug("[DetailCacheWarming] Sorted Set 비어있음 - 워밍 생략");
            return;
        }

        Cache cache = cacheManager.getCache("post:boast:detail");
        if (cache == null) return;

        int success = 0;
        for (Long id : topIds) {
            try {
                // DB에서 최신 데이터 조회 후 즉시 덮어쓰기 (공백 없음)
                GetBoastCatPostResponse fresh = popularPostService.fetchDetail(id);
                cache.put(id, fresh);
                success++;
            } catch (Exception e) {
                log.warn("[DetailCacheWarming] postId: {} 갱신 실패: {}", id, e.getMessage());
            }
        }
        log.info("[DetailCacheWarming] 완료 - {}/{}개 갱신", success, topIds.size());
    }
}
