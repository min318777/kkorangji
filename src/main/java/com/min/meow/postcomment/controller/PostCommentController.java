package com.min.meow.postcomment.controller;


import com.min.meow.postcomment.domain.dto.RegisterPostCommentDto;
import com.min.meow.postcomment.domain.dto.UpdatePostCommentDto;
import com.min.meow.postcomment.domain.request.RegisterPostCommentRequest;
import com.min.meow.postcomment.domain.request.UpdatePostCommentRequest;
import com.min.meow.postcomment.service.PostCommentService;
import com.min.meow.global.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/meow/lost-cat/comment")
public class PostCommentController {

    private final PostCommentService postCommentService;

    // 댓글 작성
    @PostMapping("/{lostCatPostId}")
    public ResponseEntity<?> registerLostCatPostComment(@RequestBody @Valid RegisterPostCommentRequest registerPostCommentRequest, @PathVariable Long lostCatPostId){

        RegisterPostCommentDto registerPostCommentDto = postCommentService.registerLostCatPostComment(registerPostCommentRequest, lostCatPostId);
        return ResponseEntity.ok(new ResponseDto<>(true, "댓글 작성 성공", registerPostCommentDto));
    }

    // 댓글 수정
    @PutMapping("/{lostCatPostCommentId}")
    public ResponseEntity<?> updateLostCatPostComment(@RequestBody @Valid UpdatePostCommentRequest updatePostCommentRequest, @PathVariable Long lostCatPostCommentId){

        UpdatePostCommentDto updatePostCommentDto = postCommentService.updateLostCatPostComment(updatePostCommentRequest, lostCatPostCommentId);
        return ResponseEntity.ok(new ResponseDto<>(true, "댓글 수정 성공", updatePostCommentDto));
    }

    // 댓글 삭제
    @DeleteMapping("/{lostCatPostCommentId}")
    public ResponseEntity<?> deleteLostCatPostComment(@PathVariable Long lostCatPostCommentId){

        postCommentService.deleteLostCatPostComment(lostCatPostCommentId);
        return ResponseEntity.ok(new ResponseDto<>(true, "댓글 삭제 성공", null));
    }


}
