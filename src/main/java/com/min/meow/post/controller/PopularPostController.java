package com.min.meow.post.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.service.PopularPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /**
     * v5: Redis Sorted Set 실시간 집계
     * 좋아요/댓글/조회수 이벤트 → ZINCRBY → 조회 시 ZRANGE (O(log N))
     * 캐시 없이 매 요청마다 Sorted Set → DB IN 조회로 최신 순위 반환
     */
    @Operation(summary = "인기 게시물 TOP 24 (v5 - Sorted Set)",
            description = "Redis Sorted Set 실시간 집계. 이벤트 기반 ZINCRBY. 캐시 없이 매 요청마다 Sorted Set 조회 후 DB IN 쿼리로 상세 조합.")
    @SecurityRequirements
    @GetMapping("/popular/v5")
    public ResponseEntity<ApiResponse<List<BoastCatPostListResponse>>> getPopularPostsV5() {
        return ResponseEntity.ok(ApiResponse.success("인기 게시물 조회 성공 (v5)", popularPostService.getPopularPostsV5()));
    }

    // ========== 인기글 상세조회 — Cache Stampede 방지 비교 ==========

    /**
     * 인기글 상세조회 v1 — 기본 @Cacheable (비교 기준선)
     * TTL 만료 시 동시 요청 → 여러 스레드가 동시에 DB 조회 (Stampede 발생)
     */
    @Operation(summary = "인기글 상세 조회 (v1 - 기본 캐시)",
            description = "기본 @Cacheable. TTL 만료 시 Stampede 발생. 비교 기준선.")
    @SecurityRequirements
    @GetMapping("/popular/detail/{id}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getPopularPostDetail(
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("인기글 상세 조회 성공 (v1)", popularPostService.getDetailV1(id)));
    }

    /**
     * 인기글 상세조회 v2 — Lettuce SETNX 분산 락
     * MISS 시 첫 스레드만 DB 조회, 나머지는 100ms 간격 대기 (최대 3초)
     */
    @Operation(summary = "인기글 상세 조회 (v2 - 분산 락)",
            description = "Redis SETNX 분산 락. MISS 시 첫 스레드만 DB 조회, 나머지 대기.")
    @SecurityRequirements
    @GetMapping("/popular/detail/v2/{id}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getPopularPostDetailV2(
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("인기글 상세 조회 성공 (v2)", popularPostService.getDetailV2(id)));
    }

    /**
     * 인기글 상세조회 v3 — Cache Warming (Stampede 원천 차단, 설계 의도)
     * 설계상 DetailCacheWarmingScheduler가 25초마다 TOP 24 상세 캐시 선제 갱신하여
     * TTL(30초) 만료 전 항상 캐시가 채워져 있어야 하나, 현재 워밍 스케줄러는 비활성 상태
     */
    @Operation(summary = "인기글 상세 조회 (v3 - Cache Warming)",
            description = "설계상 스케줄러가 25초마다 TOP 24 상세 캐시 선제 갱신해 MISS를 방지하나, 현재 워밍 스케줄러가 비활성 상태라 TTL 만료 시 MISS 발생.")
    @SecurityRequirements
    @GetMapping("/popular/detail/v3/{id}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getPopularPostDetailV3(
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("인기글 상세 조회 성공 (v3)", popularPostService.getDetailV3(id)));
    }

    /**
     * 인기글 상세조회 v4 — Redisson RLock (Stampede 방지)
     * v2(Lettuce SETNX 폴링)와 달리 Pub/Sub 기반 대기 + Lua 스크립트 원자적 해제
     */
    @Operation(summary = "인기글 상세 조회 (v4 - Redisson RLock)",
            description = "Redisson RLock. Pub/Sub 기반 대기 + Lua 스크립트 원자적 해제. MISS 시 첫 스레드만 DB 조회.")
    @SecurityRequirements
    @GetMapping("/popular/detail/v4/{id}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getPopularPostDetailV4(
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("인기글 상세 조회 성공 (v4)", popularPostService.getDetailV4(id)));
    }
}
