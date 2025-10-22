package com.min.meow.post.comment.controller;


import com.min.meow.config.PrincipalUser;
import com.min.meow.global.exception.ApiResponse;
import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.domain.response.GetCommentResponse;
import com.min.meow.post.comment.domain.response.RegisterCommentResponse;
import com.min.meow.post.comment.domain.response.UpdateCommentResponse;
import com.min.meow.post.comment.service.impl.CommentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentServiceImpl commentServiceImpl;

    // 고양이 자랑 게시글 댓글 조회
    @GetMapping("/api/meow/boast-cat/comments/{boastCatPostId}")
    public ResponseEntity<ApiResponse<List<GetCommentResponse>>> getBoastCatPostComment(@PathVariable Long boastCatPostId,
                                                                             @AuthenticationPrincipal PrincipalUser user){
        List<GetCommentResponse> getBoastCatPostResponse = commentServiceImpl.getBoastCatPostComment(boastCatPostId);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 조회 성공", getBoastCatPostResponse));
    }

    // 고양이 자랑 게시글 댓글 작성
    @PostMapping("/api/meow/boast-cat/comments/{boastCatPostId}")
    public ResponseEntity<ApiResponse<RegisterCommentResponse>> registerBoastCatPostComment(@RequestBody @Valid RegisterCommentRequest registerCommentRequest,
                                                                                           @PathVariable Long boastCatPostId,
                                                                                           @AuthenticationPrincipal PrincipalUser user){

        RegisterCommentResponse registerCommentResponse = commentServiceImpl.registerBoastCatPostComment(registerCommentRequest, boastCatPostId, user.getLoginId());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 작성 성공", registerCommentResponse));
    }

    // ==================== 실종 고양이 게시글 댓글 API ====================

    // 실종 고양이 게시글 댓글 조회
    @GetMapping("/api/meow/lost-cat/comments/{lostCatPostId}")
    public ResponseEntity<ApiResponse<List<GetCommentResponse>>> getLostCatPostComment(@PathVariable Long lostCatPostId,
                                                                                         @AuthenticationPrincipal PrincipalUser user){
        List<GetCommentResponse> getLostCatPostResponse = commentServiceImpl.getLostCatPostComment(lostCatPostId);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 조회 성공", getLostCatPostResponse));
    }

    // 실종 고양이 게시글 댓글 작성
    @PostMapping("/api/meow/lost-cat/comments/{lostCatPostId}")
    public ResponseEntity<ApiResponse<RegisterCommentResponse>> registerLostCatPostComment(@RequestBody @Valid RegisterCommentRequest registerCommentRequest,
                                                                                            @PathVariable Long lostCatPostId,
                                                                                            @AuthenticationPrincipal PrincipalUser user){

        RegisterCommentResponse registerCommentResponse = commentServiceImpl.registerLostCatPostComment(registerCommentRequest, lostCatPostId, user.getLoginId());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 작성 성공", registerCommentResponse));
    }

    // ==================== 공통 댓글 관리 API ====================

    // 댓글 수정 (게시글 타입 무관)
    @PutMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<ApiResponse<UpdateCommentResponse>> updateComment(@RequestBody @Valid UpdateCommentRequest updateCommentRequest,
                                                                                       @PathVariable Long commentId,
                                                                                       @AuthenticationPrincipal PrincipalUser user){

        UpdateCommentResponse updateCommentResponse = commentServiceImpl.updateComment(updateCommentRequest, commentId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 수정 성공", updateCommentResponse));
    }

    // 댓글 삭제 (게시글 타입 무관)
    @DeleteMapping("/api/meow/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId,
                                                                      @AuthenticationPrincipal PrincipalUser user){

        commentServiceImpl.deleteComment(commentId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 삭제 성공", null));
    }
}
