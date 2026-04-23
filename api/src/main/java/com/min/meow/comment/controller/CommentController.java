package com.min.meow.comment.controller;


import com.min.meow.global.PageResponse;
import com.min.meow.global.PrincipalUser;
import com.min.meow.global.ApiResponse;
import com.min.meow.comment.dto.request.RegisterCommentRequest;
import com.min.meow.comment.dto.request.UpdateCommentRequest;
import com.min.meow.comment.dto.response.GetCommentResponse;
import com.min.meow.comment.dto.response.RegisterCommentResponse;
import com.min.meow.comment.dto.response.UpdateCommentResponse;
import com.min.meow.comment.service.CommentService;
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

@Tag(name = "댓글", description = "게시글 댓글 CRUD API")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ==================== 자랑글 댓글 API ====================

    @Operation(summary = "자랑글 댓글 조회 (페이징)",
            description = "자랑글의 댓글을 페이징으로 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/api/meow/boast-cat/{boastCatPostId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<GetCommentResponse>>> getBoastCatPostCommentPaged(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<GetCommentResponse> comments = commentService.getBoastCatPostComment(boastCatPostId, pageable);

        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    @Deprecated
    @Operation(summary = "자랑글 댓글 조회 (전체)",
            description = "하위 호환용 API. 페이징 버전 사용을 권장합니다.",
            deprecated = true)
    @SecurityRequirements
    @GetMapping("/api/meow/boast-cat/comments/{boastCatPostId}")
    public ResponseEntity<ApiResponse<List<GetCommentResponse>>> getBoastCatPostComment(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        List<GetCommentResponse> comments = commentService.getBoastCatPostComment(boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    @Operation(summary = "자랑글 댓글 작성",
            description = "자랑글에 댓글을 작성합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('comment:write')")
    @PostMapping("/api/meow/boast-cat/comments/{boastCatPostId}")
    public ResponseEntity<ApiResponse<RegisterCommentResponse>> registerBoastCatPostComment(
            @RequestBody @Valid RegisterCommentRequest registerCommentRequest,
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        RegisterCommentResponse registerCommentResponse = commentService.registerBoastCatPostComment(registerCommentRequest, boastCatPostId, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("댓글 작성 성공", registerCommentResponse));
    }

    // ==================== 실종글 댓글 API ====================

    @Operation(summary = "실종글 댓글 조회 (페이징)",
            description = "실종글의 댓글을 페이징으로 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/api/meow/lost-cat/{lostCatPostId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<GetCommentResponse>>> getLostCatPostCommentPaged(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<GetCommentResponse> comments = commentService.getLostCatPostComment(lostCatPostId, pageable);

        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    @Deprecated
    @Operation(summary = "실종글 댓글 조회 (전체)",
            description = "하위 호환용 API. 페이징 버전 사용을 권장합니다.",
            deprecated = true)
    @SecurityRequirements
    @GetMapping("/api/meow/lost-cat/comments/{lostCatPostId}")
    public ResponseEntity<ApiResponse<List<GetCommentResponse>>> getLostCatPostComment(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        List<GetCommentResponse> comments = commentService.getLostCatPostComment(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    @Operation(summary = "실종글 댓글 작성",
            description = "실종글에 댓글을 작성합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('comment:write')")
    @PostMapping("/api/meow/lost-cat/comments/{lostCatPostId}")
    public ResponseEntity<ApiResponse<RegisterCommentResponse>> registerLostCatPostComment(
            @RequestBody @Valid RegisterCommentRequest registerCommentRequest,
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        RegisterCommentResponse registerCommentResponse = commentService.registerLostCatPostComment(registerCommentRequest, lostCatPostId, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("댓글 작성 성공", registerCommentResponse));
    }

    // ==================== 공통 댓글 관리 API ====================

    @Operation(summary = "댓글 수정",
            description = "댓글을 수정합니다. 게시글 타입에 관계없이 댓글 ID로 수정합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('comment:write')")
    @PutMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<ApiResponse<UpdateCommentResponse>> updateComment(
            @RequestBody @Valid UpdateCommentRequest updateCommentRequest,
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        UpdateCommentResponse updateCommentResponse = commentService.updateComment(updateCommentRequest, commentId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("댓글 수정 성공", updateCommentResponse));
    }

    @Operation(summary = "댓글 삭제",
            description = "댓글을 삭제합니다. 게시글 타입에 관계없이 댓글 ID로 삭제합니다. 인증 필요.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        commentService.deleteComment(commentId, user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
