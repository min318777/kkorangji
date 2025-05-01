package com.min.meow.comment.controller;


import com.min.meow.comment.domain.dto.RegisterLostCatPostCommentDto;
import com.min.meow.comment.domain.dto.UpdateLostCatPostCommentDto;
import com.min.meow.comment.domain.request.RegisterLostCatPostCommentRequest;
import com.min.meow.comment.domain.request.UpdateLostCatPostCommentRequest;
import com.min.meow.comment.service.LostCatPostCommentService;
import com.min.meow.global.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/meow/lost-cat/comment")
public class LostCatPostCommentController {

    private final LostCatPostCommentService lostCatPostCommentService;

    // 댓글 작성
    @PostMapping("/{lostCatPostId}")
    public ResponseEntity<?> registerLostCatPostComment(@RequestBody @Valid RegisterLostCatPostCommentRequest registerLostCatPostCommentRequest, @PathVariable Long lostCatPostId){

        RegisterLostCatPostCommentDto registerLostCatPostCommentDto = lostCatPostCommentService.registerLostCatPostComment(registerLostCatPostCommentRequest, lostCatPostId);
        return ResponseEntity.ok(new ResponseDto<>(true, "댓글 작성 성공", registerLostCatPostCommentDto));
    }

    // 댓글 수정
    @PutMapping("/{lostCatPostCommentId}")
    public ResponseEntity<?> updateLostCatPostComment(@RequestBody @Valid UpdateLostCatPostCommentRequest updateLostCatPostCommentRequest, @PathVariable Long lostCatPostCommentId){

        UpdateLostCatPostCommentDto updateLostCatPostCommentDto = lostCatPostCommentService.updateLostCatPostComment(updateLostCatPostCommentRequest, lostCatPostCommentId);
        return ResponseEntity.ok(new ResponseDto<>(true, "댓글 수정 성공", updateLostCatPostCommentDto));
    }

    // 댓글 삭제
    @DeleteMapping("/{lostCatPostCommentId}")
    public ResponseEntity<?> deleteLostCatPostComment(@PathVariable Long lostCatPostCommentId){

        lostCatPostCommentService.deleteLostCatPostComment(lostCatPostCommentId);
        return ResponseEntity.ok(new ResponseDto<>(true, "댓글 삭제 성공", null));
    }


}
