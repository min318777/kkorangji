package com.min.meow.postlike.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.common.PrincipalUser;
import com.min.meow.postlike.service.PostLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "좋아요", description = "게시글 좋아요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow/boast-cat")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @Operation(summary = "좋아요 등록", description = "자랑글에 좋아요를 등록합니다. 이미 좋아요한 경우 409 반환.")
    @PostMapping("/{boastCatPostId}/like")
    public ResponseEntity<ApiResponse<Long>> addLike(
            @Parameter(description = "자랑글 ID") @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {
        Long likeCount = postLikeService.addLike(boastCatPostId, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("좋아요 등록 성공", likeCount));
    }

    @Operation(summary = "좋아요 취소", description = "자랑글 좋아요를 취소합니다. 좋아요하지 않은 경우 400 반환.")
    @DeleteMapping("/{boastCatPostId}/like")
    public ResponseEntity<ApiResponse<Long>> cancelLike(
            @Parameter(description = "자랑글 ID") @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {
        Long likeCount = postLikeService.cancelLike(boastCatPostId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("좋아요 취소 성공", likeCount));
    }

    @Operation(summary = "좋아요 여부 확인", description = "현재 로그인 사용자의 좋아요 여부를 조회합니다.")
    @GetMapping("/{boastCatPostId}/like/status")
    public ResponseEntity<ApiResponse<Boolean>> isLiked(
            @Parameter(description = "자랑글 ID") @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {
        boolean liked = postLikeService.isLiked(boastCatPostId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("좋아요 여부 조회 성공", liked));
    }
}
