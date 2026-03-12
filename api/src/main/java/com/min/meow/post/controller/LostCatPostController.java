package com.min.meow.post.controller;


import com.min.meow.global.PostType;
import com.min.meow.global.PrincipalUser;
import com.min.meow.global.PageResponse;
import com.min.meow.global.ApiResponse;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.service.ViewCountService;
import com.min.meow.post.service.LostCatPostService;
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

import java.util.List;

@Tag(name = "실종글", description = "실종 고양이 신고 게시글 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow/lost-cat")
public class LostCatPostController {

    private final LostCatPostService lostCatPostService;
    private final ViewCountService viewCountService;

    @Operation(summary = "실종글 목록 조회", description = "페이징된 실종글 목록을 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LostCatPostListResponse>>> getAllLostCatPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam (defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam (defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<LostCatPostListResponse> pageResponse = lostCatPostService.getAllLostCatPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success("모든 글 조회 성공", pageResponse));
    }

    @Operation(summary = "실종글 상세 조회", description = "게시글 ID로 상세 정보를 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<GetLostCatPostResponse>> getLostCatPostDetail(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId){

        GetLostCatPostResponse lostCatPostDto = lostCatPostService.getLostCatPost(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("글 조회 성공", lostCatPostDto));
    }

    /**
     * 글 생성 (Presigned URL 기반 이미지 업로드)
     */
    @Operation(summary = "실종글 생성",
            description = "새 실종글을 작성합니다. 이미지는 Presigned URL로 S3에 먼저 업로드 후 key를 전달합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('post:write')")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateLostCatPostResponse>> createLostCatPost(
            @RequestBody @Valid CreateLostCatPostRequest createLostCatPostRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        CreateLostCatPostResponse lostCatPostDto = lostCatPostService.createLostCatPost(createLostCatPostRequest, user.getLoginId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("글 생성 성공", lostCatPostDto));
    }

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     */
    @Operation(summary = "실종글 수정",
            description = "실종글을 수정합니다. 본인 게시글만 수정 가능합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('post:write')")
    @PutMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<UpdateLostCatPostResponse>> updateLostCatPost(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            @RequestBody @Valid UpdateLostCatPostRequest updateLostCatPostRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){
        String loginId = user.getLoginId();
        UpdateLostCatPostResponse lostCatPostDto = lostCatPostService.updateLostCatPost(lostCatPostId, updateLostCatPostRequest, loginId);
        return ResponseEntity.ok(ApiResponse.success("글 수정 성공", lostCatPostDto));
    }

    @Operation(summary = "실종글 삭제",
            description = "실종글을 삭제합니다. 본인 게시글만 삭제 가능합니다. 인증 필요.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{lostCatPostId}")
    public ResponseEntity<Void> deleteLostCatPost(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        String loginId = user.getLoginId();
        lostCatPostService.deleteLostCatPost(lostCatPostId, loginId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "최근 실종글 20개 조회",
            description = "최신 실종글 20개를 Projection으로 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<LostCatPostListResponse>>> getRecentLostCatPosts() {
        List<LostCatPostListResponse> posts = lostCatPostService.getRecentLostCatPosts();
        return ResponseEntity.ok(ApiResponse.success("최근 실종글 20개 조회 성공", posts));
    }

    @Operation(summary = "조회수 증가 (v2 원자적)",
            description = "DB 원자적 쿼리로 조회수를 증가시킵니다. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId) {
        lostCatPostService.incrementViewCount(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공", null));
    }

    /**
     * @deprecated 동시성 이슈로 인해 POST /{lostCatPostId}/view 사용 권장
     */
    @Deprecated
    @Operation(summary = "조회수 증가 (v1 더티체킹)",
            description = "JPA 더티 체킹 방식. 동시성 이슈(Lost Update)가 있으므로 v2 사용을 권장합니다.",
            deprecated = true)
    @SecurityRequirements
    @PostMapping("/v1/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCountV1(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId) {
        lostCatPostService.incrementViewCountWithDirtyChecking(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공 (더티 체킹 방식)", null));
    }

    @Operation(summary = "조회수 증가 (v3 Redis)",
            description = "Redis INCR 방식. DB 부하를 줄이고 동시성을 완벽 보장합니다. Redis 장애 시 DB fallback. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/v3/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<Long>> incrementViewCountV3(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId) {
        Long newCount = viewCountService.incrementViewCount(PostType.LOST, lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공 (Redis INCR 방식)", newCount));
    }
}
