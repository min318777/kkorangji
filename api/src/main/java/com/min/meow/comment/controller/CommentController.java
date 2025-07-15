package com.min.meow.comment.controller;


import com.min.meow.comment.domain.dto.RegisterPostCommentDto;
import com.min.meow.comment.domain.dto.UpdatePostCommentDto;
import com.min.meow.comment.domain.request.RegisterPostCommentRequest;
import com.min.meow.comment.domain.request.UpdatePostCommentRequest;
import com.min.meow.comment.service.CommentService;
import com.min.meow.global.exception.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/meow/lost-cat/comment")
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @PostMapping("/{lostCatPostId}")
    public ResponseEntity<?> registerLostCatPostComment(@RequestBody @Valid RegisterPostCommentRequest registerPostCommentRequest, @PathVariable Long lostCatPostId){

        RegisterPostCommentDto registerPostCommentDto = commentService.registerLostCatPostComment(registerPostCommentRequest, lostCatPostId);
        return ResponseEntity.ok(new ErrorResponse<>(true, "댓글 작성 성공", registerPostCommentDto));
    }

    // 댓글 수정
    @PutMapping("/{lostCatPostCommentId}")
    public ResponseEntity<?> updateLostCatPostComment(@RequestBody @Valid UpdatePostCommentRequest updatePostCommentRequest, @PathVariable Long lostCatPostCommentId){

        UpdatePostCommentDto updatePostCommentDto = commentService.updateLostCatPostComment(updatePostCommentRequest, lostCatPostCommentId);
        return ResponseEntity.ok(new ErrorResponse<>(true, "댓글 수정 성공", updatePostCommentDto));
    }

    // 댓글 삭제
    @DeleteMapping("/{lostCatPostCommentId}")
    public ResponseEntity<?> deleteLostCatPostComment(@PathVariable Long lostCatPostCommentId){

        commentService.deleteLostCatPostComment(lostCatPostCommentId);
        return ResponseEntity.ok(new ErrorResponse<>(true, "댓글 삭제 성공", null));
    }


}
