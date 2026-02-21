package com.min.meow.comment.controller;


import com.min.meow.global.PageResponse;
import com.min.meow.global.PrincipalUser;
import com.min.meow.global.ApiResponse;
import com.min.meow.comment.dto.request.RegisterCommentRequest;
import com.min.meow.comment.dto.request.UpdateCommentRequest;
import com.min.meow.comment.dto.response.GetCommentResponse;
import com.min.meow.comment.dto.response.RegisterCommentResponse;
import com.min.meow.comment.dto.response.UpdateCommentResponse;
import com.min.meow.comment.service.impl.CommentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentServiceImpl commentServiceImpl;

    /**
     * 고양이 자랑 게시글 댓글 조회 (페이지네이션)
     * - 댓글은 게시글 상세와 분리되어 별도 API로 조회
     * - 프론트엔드에서 무한 스크롤 또는 페이지네이션 UI 구현 가능
     */
    @GetMapping("/api/meow/boast-cat/{boastCatPostId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<GetCommentResponse>>> getBoastCatPostCommentPaged(
            @PathVariable Long boastCatPostId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<GetCommentResponse> comments = commentServiceImpl.getBoastCatPostComment(boastCatPostId, pageable);

        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    // 고양이 자랑 게시글 댓글 조회 (전체 - 하위 호환용)
    @GetMapping("/api/meow/boast-cat/comments/{boastCatPostId}")
    public ResponseEntity<ApiResponse<List<GetCommentResponse>>> getBoastCatPostComment(
            @PathVariable Long boastCatPostId,
            @AuthenticationPrincipal PrincipalUser user) {

        List<GetCommentResponse> comments = commentServiceImpl.getBoastCatPostComment(boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    // 고양이 자랑 게시글 댓글 작성
    @PostMapping("/api/meow/boast-cat/comments/{boastCatPostId}")
    public ResponseEntity<ApiResponse<RegisterCommentResponse>> registerBoastCatPostComment(@RequestBody @Valid RegisterCommentRequest registerCommentRequest,
                                                                                           @PathVariable Long boastCatPostId,
                                                                                           @AuthenticationPrincipal PrincipalUser user){

        RegisterCommentResponse registerCommentResponse = commentServiceImpl.registerBoastCatPostComment(registerCommentRequest, boastCatPostId, user.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("댓글 작성 성공", registerCommentResponse));
    }

    // ==================== 실종 고양이 게시글 댓글 API ====================

    /**
     * 실종 고양이 게시글 댓글 조회 (페이지네이션)
     * - 댓글은 게시글 상세와 분리되어 별도 API로 조회
     * - 프론트엔드에서 무한 스크롤 또는 페이지네이션 UI 구현 가능
     */
    @GetMapping("/api/meow/lost-cat/{lostCatPostId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<GetCommentResponse>>> getLostCatPostCommentPaged(
            @PathVariable Long lostCatPostId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<GetCommentResponse> comments = commentServiceImpl.getLostCatPostComment(lostCatPostId, pageable);

        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    // 실종 고양이 게시글 댓글 조회 (전체 - 하위 호환용)
    @GetMapping("/api/meow/lost-cat/comments/{lostCatPostId}")
    public ResponseEntity<ApiResponse<List<GetCommentResponse>>> getLostCatPostComment(
            @PathVariable Long lostCatPostId,
            @AuthenticationPrincipal PrincipalUser user) {

        List<GetCommentResponse> comments = commentServiceImpl.getLostCatPostComment(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("댓글 조회 성공", comments));
    }

    // 실종 고양이 게시글 댓글 작성
    @PostMapping("/api/meow/lost-cat/comments/{lostCatPostId}")
    public ResponseEntity<ApiResponse<RegisterCommentResponse>> registerLostCatPostComment(@RequestBody @Valid RegisterCommentRequest registerCommentRequest,
                                                                                            @PathVariable Long lostCatPostId,
                                                                                            @AuthenticationPrincipal PrincipalUser user){

        RegisterCommentResponse registerCommentResponse = commentServiceImpl.registerLostCatPostComment(registerCommentRequest, lostCatPostId, user.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("댓글 작성 성공", registerCommentResponse));
    }

    // ==================== 공통 댓글 관리 API ====================

    // 댓글 수정 (게시글 타입 무관)
    @PutMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<ApiResponse<UpdateCommentResponse>> updateComment(@RequestBody @Valid UpdateCommentRequest updateCommentRequest,
                                                                                       @PathVariable Long commentId,
                                                                                       @AuthenticationPrincipal PrincipalUser user){

        UpdateCommentResponse updateCommentResponse = commentServiceImpl.updateComment(updateCommentRequest, commentId);
        return ResponseEntity.ok(ApiResponse.success("댓글 수정 성공", updateCommentResponse));
    }

    // 댓글 삭제 (게시글 타입 무관)
    @DeleteMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId,
                                                                      @AuthenticationPrincipal PrincipalUser user){

        commentServiceImpl.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
