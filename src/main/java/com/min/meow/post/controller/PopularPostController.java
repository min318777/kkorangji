package com.min.meow.post.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.service.PopularPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "인기글", description = "인기 게시글 목록/상세 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow/boast-cat-posts")
public class PopularPostController {

    private final PopularPostService popularPostService;

    // v1: 기본 @Cacheable — Stampede 방지 없음 (비교 기준선)
    @Operation(summary = "인기 게시물 TOP 24 (v1 - 무방지)",
            description = "기본 @Cacheable. TTL 30초. Stampede 방지 없음. 비교 기준선.")
    @SecurityRequirements
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<BoastCatPostListResponse>>> getPopularPosts() {
        return ResponseEntity.ok(ApiResponse.success("인기 게시물 조회 성공 (v1)", popularPostService.getPopularPosts()));
    }

    // v5: Redis Sorted Set 실시간 집계 — 좋아요/댓글/조회수 이벤트로 ZINCRBY, 조회 시 ZRANGE(O(log N)) 후 DB IN 조회
    @Operation(summary = "인기 게시물 TOP 24 (v5 - Sorted Set)",
            description = "Redis Sorted Set 실시간 집계. 이벤트 기반 ZINCRBY. 캐시 없이 매 요청마다 Sorted Set 조회 후 DB IN 쿼리로 상세 조합.")
    @SecurityRequirements
    @GetMapping("/popular/v5")
    public ResponseEntity<ApiResponse<List<BoastCatPostListResponse>>> getPopularPostsV5() {
        return ResponseEntity.ok(ApiResponse.success("인기 게시물 조회 성공 (v5)", popularPostService.getPopularPostsV5()));
    }

}
