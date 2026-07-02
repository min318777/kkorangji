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

@Tag(name = "인기글", description = "인기 게시글 목록 조회 API (Cache Stampede 방지 비교)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow/boast-cat")
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

    // v2: Lettuce SETNX 분산 락 — Stampede 방지
    @Operation(summary = "인기 게시물 TOP 24 (v2 - 분산 락)",
            description = "Redis SETNX 분산 락. MISS 시 첫 스레드만 DB 조회, 나머지 대기.")
    @SecurityRequirements
    @GetMapping("/popular/v2")
    public ResponseEntity<ApiResponse<List<BoastCatPostListResponse>>> getPopularPostsV2() {
        return ResponseEntity.ok(ApiResponse.success("인기 게시물 조회 성공 (v2)", popularPostService.getPopularPostsV2()));
    }

    // v3: Cache Warming — 스케줄러가 25초마다 선제 갱신
    @Operation(summary = "인기 게시물 TOP 24 (v3 - Cache Warming)",
            description = "스케줄러가 25초마다 미리 갱신. MISS 자체 발생 안 함.")
    @SecurityRequirements
    @GetMapping("/popular/v3")
    public ResponseEntity<ApiResponse<List<BoastCatPostListResponse>>> getPopularPostsV3() {
        return ResponseEntity.ok(ApiResponse.success("인기 게시물 조회 성공 (v3)", popularPostService.getPopularPostsV3()));
    }

    // v4: Redisson RLock — Stampede 방지
    @Operation(summary = "인기 게시물 TOP 24 (v4 - Redisson RLock)",
            description = "Redisson RLock 기반 분산 락. 소유자 검증 + Lua 스크립트 원자적 해제.")
    @SecurityRequirements
    @GetMapping("/popular/v4")
    public ResponseEntity<ApiResponse<List<BoastCatPostListResponse>>> getPopularPostsV4() {
        return ResponseEntity.ok(ApiResponse.success("인기 게시물 조회 성공 (v4)", popularPostService.getPopularPostsV4()));
    }

    /**
     * v5: Redis Sorted Set 실시간 집계
     * 좋아요/댓글/조회수 이벤트 → ZINCRBY → 조회 시 ZRANGE (O(log N))
     * DB 집계 쿼리 없이 실시간 랭킹 반환
     */
    @Operation(summary = "인기 게시물 TOP 24 (v5 - Sorted Set)",
            description = "Redis Sorted Set 실시간 집계. 이벤트 기반 ZINCRBY. DB 집계 쿼리 없음.")
    @SecurityRequirements
    @GetMapping("/popular/v5")
    public ResponseEntity<ApiResponse<List<BoastCatPostListResponse>>> getPopularPostsV5() {
        return ResponseEntity.ok(ApiResponse.success("인기 게시물 조회 성공 (v5)", popularPostService.getPopularPostsV5()));
    }
}
