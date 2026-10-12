package com.min.meow.post.service;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.repository.PopularPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularPostService {

    private final PopularPostRepository popularPostRepository;
    private final PopularRankingService popularRankingService;

    // v1: 기본 @Cacheable
    public List<BoastCatPostListResponse> getPopularPosts() {
        log.debug("[v1 Cache MISS] DB 조회 - thread: {}", Thread.currentThread().getName());
        return popularPostRepository.findTop24ByScore();
    }

    // v5: Redis Sorted Set 실시간 집계 — 캐시 없이 매 요청마다 Sorted Set → DB 조회로 최신 순위 반환
    public List<BoastCatPostListResponse> getPopularPostsV5() {
        List<Long> ids = popularRankingService.getTop24PostIds();

        if (ids.isEmpty()) {
            log.debug("[v5 Sorted Set 비어있음] DB 직접 조회 fallback");
            return popularPostRepository.findTop24ByScore();
        }

        // ID 목록으로 DB 조회 후 Sorted Set 순서(score 내림차순)로 재정렬
        List<BoastCatPostListResponse> posts = popularPostRepository.findByIds(ids);
        Map<Long, BoastCatPostListResponse> postMap = posts.stream()
                .collect(Collectors.toMap(BoastCatPostListResponse::getId, p -> p));

        return ids.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
