package com.min.meow.postlike.controller;


import com.min.meow.global.exception.ErrorResponse;
import com.min.meow.postlike.service.PostLikeService;
import com.min.meow.config.PrincipalUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/like")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @GetMapping("/{boastCatPostId}")
    public ResponseEntity<?> getLikeCount(@PathVariable Long boastCatPostId) {

        int count = postLikeService.getLikeCount(boastCatPostId);
        return ResponseEntity.ok(new ErrorResponse<>(true, "좋아요 수 조회 성공", count));
    }

    @PostMapping("/{boastCatPostId}")
    public ResponseEntity<?> plusLike(@PathVariable Long boastCatPostId, @AuthenticationPrincipal PrincipalUser user){

        String loginId = user.getUser().getLoginId();
        boolean likeOrCancel = postLikeService.toggleLike(boastCatPostId, loginId);
        String message;
        if(likeOrCancel){
            message = "좋아요 등록";
        }else {
            message = "좋아요 취소";
        }
        return ResponseEntity.ok(new ErrorResponse<>(true, message, null));
    }
}
