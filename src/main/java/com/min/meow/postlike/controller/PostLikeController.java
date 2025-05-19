package com.min.meow.postlike.controller;


import com.min.meow.boastcatpost.entity.BoastCatPost;
import com.min.meow.global.ResponseDto;
import com.min.meow.global.exception.CustomException;
import com.min.meow.postlike.service.PostLikeService;
import com.min.meow.user.domain.CustomUserDetails;
import com.min.meow.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @GetMapping("/like/{boastCatPostId}")
    public ResponseEntity<?> getLikeCount(@PathVariable Long boastCatPostId) {

        int count = postLikeService.getLikeCount(boastCatPostId);
        return ResponseEntity.ok(new ResponseDto<>(true, "좋아요 수 조회 성공", count));
    }

    @PostMapping("/like/{boastCatPostId}")
    public ResponseEntity<?> plusLike(@PathVariable Long boastCatPostId, @AuthenticationPrincipal CustomUserDetails user){

        boolean likeOrCancel = postLikeService.plusLike(boastCatPostId, user.getUser().getLoginId(), user.getUser());
        String message;
        if(likeOrCancel){
            message = "좋아요 등록";
        }else {
            message = "좋아요 취소";
        }
        return ResponseEntity.ok(new ResponseDto<>(true, message, null));
    }
}
