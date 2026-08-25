package com.min.meow.search.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.common.PageResponse;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.search.dto.request.PostLikeSearchRequest;
import com.min.meow.search.dto.request.PostSearchRequest;
import com.min.meow.search.service.PostSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    @GetMapping("/api/meow/boast-cat-posts/search")
    public ResponseEntity<ApiResponse<PageResponse<BoastCatPostListResponse>>> searchByFts(
            @Valid @ModelAttribute PostSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BoastCatPostListResponse> posts = postSearchService.searchByFts(request, pageable);
        return ResponseEntity.ok(ApiResponse.success("FTS 검색 성공", PageResponse.from(posts)));
    }

    @Operation(summary = "자랑글 FTS 검색 (자연어 모드)", description = "MySQL FTS NATURAL LANGUAGE MODE. 암묵적 OR + 관련도 점수 기반, 연산자 미지원. keyword 2글자 이상 필수.")
    @SecurityRequirements
    @GetMapping("/api/meow/boast-cat-posts/search/natural")
    public ResponseEntity<ApiResponse<PageResponse<BoastCatPostListResponse>>> searchByNaturalLanguage(
            @Valid @ModelAttribute PostSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BoastCatPostListResponse> posts = postSearchService.searchByNaturalLanguage(request, pageable);
        return ResponseEntity.ok(ApiResponse.success("자연어 모드 검색 성공", PageResponse.from(posts)));
    }

    @Operation(summary = "자랑글 LIKE 검색 (성능 비교용)", description = "LIKE '%keyword%' 방식. Full Table Scan. 예: ?title=고양이&contents=귀여운")
    @SecurityRequirements
    @GetMapping("/api/meow/boast-cat-posts/search/like")
    public ResponseEntity<ApiResponse<PageResponse<BoastCatPostListResponse>>> searchByLike(
            @ModelAttribute PostLikeSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<BoastCatPostListResponse> posts = postSearchService.searchByLike(request, pageable);
        return ResponseEntity.ok(ApiResponse.success("LIKE 검색 성공", PageResponse.from(posts)));
    }

    @Operation(summary = "실종글 FTS 검색", description = "MySQL ngram FTS. keyword 2글자 이상 필수. 예: ?keyword=나비")
    @SecurityRequirements
    @GetMapping("/api/meow/lost-cat-posts/search")
    public ResponseEntity<ApiResponse<PageResponse<LostCatPostListResponse>>> searchLostByFts(
            @Valid @ModelAttribute PostSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<LostCatPostListResponse> posts = postSearchService.searchLostByFts(request, pageable);
        return ResponseEntity.ok(ApiResponse.success("실종글 FTS 검색 성공", PageResponse.from(posts)));
    }

    @Operation(summary = "실종글 LIKE 검색 (성능 비교용)", description = "LIKE '%keyword%' 방식. Full Table Scan.")
    @SecurityRequirements
    @GetMapping("/api/meow/lost-cat-posts/search/like")
    public ResponseEntity<ApiResponse<PageResponse<LostCatPostListResponse>>> searchLostByLike(
            @ModelAttribute PostLikeSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<LostCatPostListResponse> posts = postSearchService.searchLostByLike(request, pageable);
        return ResponseEntity.ok(ApiResponse.success("실종글 LIKE 검색 성공", PageResponse.from(posts)));
    }
}
