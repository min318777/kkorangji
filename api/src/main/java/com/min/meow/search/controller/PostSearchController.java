package com.min.meow.search.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.search.dto.request.PostLikeSearchRequest;
import com.min.meow.search.dto.request.PostSearchRequest;
import com.min.meow.search.service.PostSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 검색은 조회 액션 → GET + 쿼리 파라미터가 RESTful 원칙
// GET이면 브라우저/CDN 캐싱 가능, URL 공유·북마크 가능

@Tag(name = "검색", description = "게시글 검색 API (FTS vs LIKE 성능 비교)")
@RestController
@RequiredArgsConstructor
public class PostSearchController {

    private final PostSearchService postSearchService;

    @Operation(summary = "자랑글 FTS 검색", description = "MySQL ngram FTS. keyword 2글자 이상 필수. 예: ?keyword=고양이")
    @SecurityRequirements
    @GetMapping("/api/meow/boast-cat/search")
    public ResponseEntity<ApiResponse<Page<BoastCatPostListResponse>>> searchByFts(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @ModelAttribute PostSearchRequest request) {
        Page<BoastCatPostListResponse> posts = postSearchService.searchByFts(request, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("FTS 검색 성공", posts));
    }

    @Operation(summary = "자랑글 LIKE 검색 (성능 비교용)", description = "LIKE '%keyword%' 방식. Full Table Scan. 예: ?title=고양이&contents=귀여운")
    @SecurityRequirements
    @GetMapping("/api/meow/boast-cat/search/like")
    public ResponseEntity<ApiResponse<Page<BoastCatPostListResponse>>> searchByLike(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @ModelAttribute PostLikeSearchRequest request) {
        Page<BoastCatPostListResponse> posts = postSearchService.searchByLike(request, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("LIKE 검색 성공", posts));
    }

    @Operation(summary = "실종글 FTS 검색", description = "MySQL ngram FTS. keyword 2글자 이상 필수. 예: ?keyword=나비")
    @SecurityRequirements
    @GetMapping("/api/meow/lost-cat/search")
    public ResponseEntity<ApiResponse<Page<LostCatPostListResponse>>> searchLostByFts(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @ModelAttribute PostSearchRequest request) {
        Page<LostCatPostListResponse> posts = postSearchService.searchLostByFts(request, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("실종글 FTS 검색 성공", posts));
    }

    @Operation(summary = "실종글 LIKE 검색 (성능 비교용)", description = "LIKE '%keyword%' 방식. Full Table Scan.")
    @SecurityRequirements
    @GetMapping("/api/meow/lost-cat/search/like")
    public ResponseEntity<ApiResponse<Page<LostCatPostListResponse>>> searchLostByLike(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @ModelAttribute PostLikeSearchRequest request) {
        Page<LostCatPostListResponse> posts = postSearchService.searchLostByLike(request, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("실종글 LIKE 검색 성공", posts));
    }
}
