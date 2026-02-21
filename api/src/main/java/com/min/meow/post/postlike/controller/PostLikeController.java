package com.min.meow.post.postlike.controller;


import com.min.meow.global.ApiResponse;
import com.min.meow.global.PostType;
import com.min.meow.post.postlike.dto.LikeResponse;
import com.min.meow.post.postlike.service.LikeCountService;
import com.min.meow.post.postlike.service.PostLikeService;
import com.min.meow.global.PrincipalUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "좋아요", description = "게시글 좋아요 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/like")
public class PostLikeController {

    private final PostLikeService postLikeService;
    private final LikeCountService likeCountService;

    // ==================== v1: 기존 더티 체킹 방식 (동시성 이슈 있음) ====================

    /**
     * @deprecated 성능 이슈로 인해 GET /v2/{boastCatPostId} 사용 권장
     */
    @Deprecated
    @Operation(summary = "좋아요 수 조회 (v1)",
            description = "Entity 로딩 방식. 성능 이슈로 v2 사용을 권장합니다.",
            deprecated = true)
    @SecurityRequirements
    @GetMapping("/{boastCatPostId}")
    public ResponseEntity<?> getLikeCount(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId) {

        int count = postLikeService.getLikeCount(boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 수 조회 성공", count));
    }

    /**
     * @deprecated 동시성 이슈로 인해 POST /v2/{boastCatPostId} 사용 권장
     */
    @Deprecated
    @Operation(summary = "좋아요 토글 (v1)",
            description = "더티 체킹 방식. 동시성 이슈(Lost Update)가 있으므로 v2 사용을 권장합니다.",
            deprecated = true)
    @PostMapping("/{boastCatPostId}")
    public ResponseEntity<?> plusLike(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        String loginId = user.getUser().getLoginId();
        boolean likeOrCancel = postLikeService.toggleLike(boastCatPostId, loginId);
        String message;
        if(likeOrCancel){
            message = "좋아요 등록";
        }else {
            message = "좋아요 취소";
        }
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    // ==================== v2: Redis + 배치 동기화 방식 (권장) ====================

    @Operation(summary = "좋아요 토글 (v2 Redis)",
            description = "Redis SET 기반 좋아요 토글. 동시성 안전하고 초고속(~0.1ms) 응답합니다. 인증 필요.")
    @PostMapping("/v2/{boastCatPostId}")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLikeV2(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        Long userId = user.getUser().getId();
        boolean liked = likeCountService.toggleLike(PostType.BOAST, boastCatPostId, userId);
        Long likeCount = likeCountService.getLikeCount(PostType.BOAST, boastCatPostId);

        String message = liked ? "좋아요 등록 성공" : "좋아요 취소 성공";
        LikeResponse response = new LikeResponse(liked, likeCount);

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "좋아요 수 조회 (v2 Redis)",
            description = "Redis SCARD로 O(1) 시간에 좋아요 수를 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/v2/{boastCatPostId}")
    public ResponseEntity<ApiResponse<Long>> getLikeCountV2(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId) {
        Long count = likeCountService.getLikeCount(PostType.BOAST, boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 수 조회 성공", count));
    }

    @Operation(summary = "좋아요 여부 확인 (v2 Redis)",
            description = "현재 사용자의 좋아요 여부를 확인합니다. Redis SISMEMBER로 O(1) 조회. 인증 필요.")
    @GetMapping("/v2/{boastCatPostId}/status")
    public ResponseEntity<ApiResponse<Boolean>> isLikedV2(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        Long userId = user.getUser().getId();
        boolean liked = likeCountService.isLiked(PostType.BOAST, boastCatPostId, userId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 여부 조회 성공", liked));
    }
}
