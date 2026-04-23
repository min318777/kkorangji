package com.min.meow.post.postlike.controller;

import com.min.meow.global.ApiResponse;
import com.min.meow.global.PostType;
import com.min.meow.post.postlike.dto.LikeResponse;
import com.min.meow.post.postlike.service.LikeCountService;
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

    private final LikeCountService likeCountService;

    // ==================== v1: Deprecated (내부적으로 v2와 동일한 로직 사용) ====================

    @Deprecated
    @Operation(summary = "좋아요 수 조회 (v1)",
            description = "v2와 동일한 로직. 하위 호환성을 위해 유지됩니다.",
            deprecated = true)
    @SecurityRequirements
    @GetMapping("/{boastCatPostId}")
    public ResponseEntity<?> getLikeCount(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId) {
        Long count = likeCountService.getLikeCount(PostType.BOAST, boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 수 조회 성공", count));
    }

    @Deprecated
    @Operation(summary = "좋아요 토글 (v1)",
            description = "v2와 동일한 로직. 하위 호환성을 위해 유지됩니다.",
            deprecated = true)
    @PostMapping("/{boastCatPostId}")
    public ResponseEntity<?> plusLike(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {
        Long userId = user.getUserId();
        boolean liked = likeCountService.toggleLike(PostType.BOAST, boastCatPostId, userId);
        String message = liked ? "좋아요 등록" : "좋아요 취소";
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    // ==================== v2: Redis + @Async DB 기록 (현재 방식) ====================

    @Operation(summary = "좋아요 토글 (v2)",
            description = "Redis SET 기반 좋아요 토글. 즉각 응답 후 DB 비동기 기록. 인증 필요.")
    @PostMapping("/v2/{boastCatPostId}")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLikeV2(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {
        Long userId = user.getUserId();
        boolean liked = likeCountService.toggleLike(PostType.BOAST, boastCatPostId, userId);
        Long likeCount = likeCountService.getLikeCount(PostType.BOAST, boastCatPostId);

        String message = liked ? "좋아요 등록 성공" : "좋아요 취소 성공";
        LikeResponse response = new LikeResponse(liked, likeCount);
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "좋아요 수 조회 (v2)",
            description = "Redis SCARD로 O(1) 조회. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/v2/{boastCatPostId}")
    public ResponseEntity<ApiResponse<Long>> getLikeCountV2(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId) {
        Long count = likeCountService.getLikeCount(PostType.BOAST, boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 수 조회 성공", count));
    }

    @Operation(summary = "좋아요 여부 확인 (v2)",
            description = "Redis SISMEMBER로 O(1) 조회. 인증 필요.")
    @GetMapping("/v2/{boastCatPostId}/status")
    public ResponseEntity<ApiResponse<Boolean>> isLikedV2(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {
        Long userId = user.getUserId();
        boolean liked = likeCountService.isLiked(PostType.BOAST, boastCatPostId, userId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 여부 조회 성공", liked));
    }
}
