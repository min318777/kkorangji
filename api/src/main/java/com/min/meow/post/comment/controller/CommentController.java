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
@RequestMapping("/api/meow/boast-cat/comments")
public class CommentController {

    private final CommentServiceImpl commentServiceImpl;

    // 댓글 조회
    @GetMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<List<GetCommentResponse>>> getBoastCatPostComment(@PathVariable Long boastCatPostId,
                                                                             @AuthenticationPrincipal PrincipalUser user){
        List<GetCommentResponse> getBoastCatPostResponse = commentServiceImpl.getBoastCatPostComment(boastCatPostId);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 조회 성공", getBoastCatPostResponse));
    }

    // 댓글 작성
    @PostMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<RegisterCommentResponse>> registerLostCatPostComment(@RequestBody @Valid RegisterCommentRequest registerCommentRequest,
                                                                                           @PathVariable Long boastCatPostId,
                                                                                           @AuthenticationPrincipal PrincipalUser user){

        RegisterCommentResponse registerCommentResponse = commentServiceImpl.registerBoastCatPostComment(registerCommentRequest, boastCatPostId, user.getLoginId());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 작성 성공", registerCommentResponse));
    }

    // 댓글 수정
    @PutMapping("/{lostCatPostCommentId}")
    public ResponseEntity<ApiResponse<UpdateCommentResponse>> updateLostCatPostComment(@RequestBody @Valid UpdateCommentRequest updateCommentRequest,
                                                                                       @PathVariable Long lostCatPostCommentId,
                                                                                       @AuthenticationPrincipal PrincipalUser user){

        UpdateCommentResponse updateCommentResponse = commentServiceImpl.updateLostCatPostComment(updateCommentRequest, lostCatPostCommentId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 수정 성공", updateCommentResponse));
    }

    // 댓글 삭제
    @DeleteMapping("/{lostCatPostCommentId}")
    public ResponseEntity<ApiResponse<Void>> deleteLostCatPostComment(@PathVariable Long lostCatPostCommentId,
                                                                      @AuthenticationPrincipal PrincipalUser user){

        commentServiceImpl.deleteLostCatPostComment(lostCatPostCommentId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 삭제 성공", null));
    }


}
