package com.min.meow.post.comment.controller;


import com.min.meow.post.comment.domain.response.RegisterCommentDto;
import com.min.meow.post.comment.domain.response.UpdateCommentDto;
import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.service.impl.CommentServiceImpl;
import com.min.meow.config.PrincipalUser;
import com.min.meow.global.exception.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/meow/lost-cat/comments")
public class CommentController {

    private final CommentServiceImpl commentServiceImpl;

    // 댓글 작성
    @PostMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<RegisterCommentDto>> registerLostCatPostComment(@RequestBody @Valid RegisterCommentRequest registerCommentRequest,
                                                                                      @PathVariable Long lostCatPostId,
                                                                                      @AuthenticationPrincipal PrincipalUser user){

        RegisterCommentDto registerCommentDto = commentServiceImpl.registerLostCatPostComment(registerCommentRequest, lostCatPostId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 작성 성공", registerCommentDto));
    }

    // 댓글 수정
    @PutMapping("/{lostCatPostCommentId}")
    public ResponseEntity<ApiResponse<UpdateCommentDto>> updateLostCatPostComment(@RequestBody @Valid UpdateCommentRequest updateCommentRequest,
                                                                                  @PathVariable Long lostCatPostCommentId,
                                                                                  @AuthenticationPrincipal PrincipalUser user){

        UpdateCommentDto updateCommentDto = commentServiceImpl.updateLostCatPostComment(updateCommentRequest, lostCatPostCommentId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 수정 성공", updateCommentDto));
    }

    // 댓글 삭제
    @DeleteMapping("/{lostCatPostCommentId}")
    public ResponseEntity<ApiResponse<Void>> deleteLostCatPostComment(@PathVariable Long lostCatPostCommentId,
                                                                      @AuthenticationPrincipal PrincipalUser user){

        commentServiceImpl.deleteLostCatPostComment(lostCatPostCommentId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 삭제 성공", null));
    }


}
