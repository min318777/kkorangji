package com.min.meow.comment.controller;


import com.min.meow.comment.domain.dto.RegisterCommentDto;
import com.min.meow.comment.domain.request.RegisterCommentRequest;
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
    public ResponseEntity<?> registerComment(@RequestBody @Valid RegisterCommentRequest registerCommentRequest, @PathVariable Long lostCatPostId){

        RegisterCommentDto registerCommentDto = lostCatPostCommentService.registerComment(registerCommentRequest, lostCatPostId);
        return ResponseEntity.ok(new ResponseDto<>(true, "댓글 작성 성공", registerCommentDto));
    }


}
