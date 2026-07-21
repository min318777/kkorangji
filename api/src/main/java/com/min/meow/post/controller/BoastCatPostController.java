package com.min.meow.post.controller;

import com.min.meow.common.PrincipalUser;
import com.min.meow.common.PageResponse;
import com.min.meow.common.ApiResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import com.min.meow.post.service.BoastCatPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;


@Tag(name = "자랑글", description = "고양이 자랑 게시글 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow/boast-cat")
public class BoastCatPostController {
    private final BoastCatPostService boastCatPostService;

    // ========== CRUD ==========

    @Operation(summary = "자랑글 목록 조회", description = "페이징된 자랑글 목록을 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BoastCatPostListResponse>>> getAllBoastCatPost(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam (defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam (defaultValue = "10") int size ){

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<BoastCatPostListResponse> posts = boastCatPostService.getAllBoastCatPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success("모든 글 조회 성공", posts));
    }

    @Operation(summary = "자랑글 상세 조회", description = "게시글 ID로 상세 정보를 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getBoastPostId(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId){
        GetBoastCatPostResponse getBoastCatPostResponse = boastCatPostService.getBoastCatPost(boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("글 조회 성공", getBoastCatPostResponse));
    }

    /**
     * 글 생성 (Presigned URL 기반 이미지 업로드)
     * 이미지 업로드 플로우:
     * 1. 클라이언트가 /api/images/presigned-urls 로 Presigned URL 요청
     * 2. 클라이언트가 Presigned URL로 S3에 이미지 직접 업로드
     * 3. 업로드 완료 후 받은 S3 key를 imageKeys에 담아서 이 API 호출
     */
    @Operation(summary = "자랑글 생성",
            description = "새 자랑글을 작성합니다. 이미지는 Presigned URL로 S3에 먼저 업로드 후 key를 전달합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('post:write')")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateBoastCatPostResponse>> createBoastCatPost(
            @RequestBody @Valid CreateBoastCatPostRequest createBoastCatPostRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){
        CreateBoastCatPostResponse post = boastCatPostService.createBoastCatPost(createBoastCatPostRequest, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("글 생성 성공", post));
    }

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     * 이미지 처리:
     * - newImageKeys: 새로 업로드된 이미지의 S3 key
     * - keepImageUrls: 유지할 기존 이미지의 CloudFront URL
     * - deleteImageUrls: 삭제할 이미지의 CloudFront URL
     */
    @Operation(summary = "자랑글 수정",
            description = "자랑글을 수정합니다. 본인 게시글만 수정 가능합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('post:write')")
    @PutMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<UpdateBoastCatPostResponse>> updateBoastCatPost(
            @RequestBody @Valid UpdateBoastCatPostRequest updateBoastCatPostRequest,
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        Long userId = user.getUserId();
        UpdateBoastCatPostResponse post = boastCatPostService.updateBoastCatPost(updateBoastCatPostRequest, boastCatPostId, userId);
        return ResponseEntity.ok(ApiResponse.success("글 수정 성공", post));
    }

    @Operation(summary = "자랑글 삭제",
            description = "자랑글을 삭제합니다. 본인 게시글만 삭제 가능합니다. 인증 필요.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{boastCatPostId}")
    public ResponseEntity<Void> deleteBoastCatPost(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        Long userId = user.getUserId();
        boastCatPostService.deleteBoastCatPost(boastCatPostId, userId);
        return ResponseEntity.noContent().build();
    }

    // ========== 상세조회 + 조회수 통합 API (v1~v4) ==========

    @Operation(summary = "자랑글 상세조회 + 조회수 증가 (v1 더티체킹)")
    @SecurityRequirements
    @GetMapping("/view/v1/{boastCatPostId}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getBoastCatPostV1(
            @Parameter(description = "자랑글 ID") @PathVariable Long boastCatPostId) {
        return ResponseEntity.ok(ApiResponse.success("상세조회 성공 (v1)", boastCatPostService.getBoastCatPostV1(boastCatPostId)));
    }

    @Operation(summary = "자랑글 상세조회 + 조회수 증가 (v2 원자적 UPDATE)")
    @SecurityRequirements
    @GetMapping("/view/v2/{boastCatPostId}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getBoastCatPostV2(
            @Parameter(description = "자랑글 ID") @PathVariable Long boastCatPostId) {
        return ResponseEntity.ok(ApiResponse.success("상세조회 성공 (v2)", boastCatPostService.getBoastCatPostV2(boastCatPostId)));
    }

    @Operation(summary = "자랑글 상세조회 + 조회수 증가 (v3 Redis INCR)")
    @SecurityRequirements
    @GetMapping("/view/v3/{boastCatPostId}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getBoastCatPostV3(
            @Parameter(description = "자랑글 ID") @PathVariable Long boastCatPostId,
            HttpServletRequest request) {
        String clientIp = request.getHeader("X-Real-IP") != null
                ? request.getHeader("X-Real-IP")
                : request.getRemoteAddr();
        return ResponseEntity.ok(ApiResponse.success("상세조회 성공 (v3)", boastCatPostService.getBoastCatPostV3(boastCatPostId, clientIp)));
    }

    @Operation(summary = "자랑글 상세조회 + 조회수 증가 (v4 비관적 락)")
    @SecurityRequirements
    @GetMapping("/view/v4/{boastCatPostId}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getBoastCatPostV4(
            @Parameter(description = "자랑글 ID") @PathVariable Long boastCatPostId) {
        return ResponseEntity.ok(ApiResponse.success("상세조회 성공 (v4)", boastCatPostService.getBoastCatPostV4(boastCatPostId)));
    }

}