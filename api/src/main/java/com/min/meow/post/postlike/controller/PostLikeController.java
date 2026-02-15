package com.min.meow.post.postlike.controller;


import com.min.meow.global.ApiResponse;
import com.min.meow.global.PostType;
import com.min.meow.global.exception.ErrorResponse;
import com.min.meow.post.postlike.dto.LikeResponse;
import com.min.meow.post.postlike.service.LikeCountService;
import com.min.meow.post.postlike.service.PostLikeService;
import com.min.meow.global.PrincipalUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/like")
public class PostLikeController {

    private final PostLikeService postLikeService;
    private final LikeCountService likeCountService;

    // ==================== v1: 기존 더티 체킹 방식 (동시성 이슈 있음) ====================

    /**
     * 좋아요 수 조회 - v1 (Entity 로딩 방식)
     *
     * @deprecated 성능 이슈로 인해 GET /v2/{boastCatPostId} 사용 권장
     */
    @GetMapping("/{boastCatPostId}")
    public ResponseEntity<?> getLikeCount(@PathVariable Long boastCatPostId) {

        int count = postLikeService.getLikeCount(boastCatPostId);
        return ResponseEntity.ok(new ErrorResponse<>(true, "좋아요 수 조회 성공", count));
    }

    /**
     * 좋아요 토글 - v1 (더티 체킹 방식)
     *
     * ⚠️ 동시성 문제 (Lost Update):
     * Entity의 likeCount++ 연산이 Read-Modify-Write 패턴으로 동작하여
     * 동시 요청 시 일부 업데이트가 손실될 수 있습니다.
     *
     * @deprecated 동시성 이슈로 인해 POST /v2/{boastCatPostId} 사용 권장
     */
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

    // ==================== v2: Redis + 배치 동기화 방식 (권장) ====================

    /**
     * 좋아요 토글 API - Redis 방식 (v2 - 최적화 버전)
     *
     * Redis SET을 사용하여 좋아요 상태를 관리합니다.
     * DB 부하를 대폭 줄이고, 동시성 문제를 완벽하게 해결합니다.
     *
     * 동작 방식:
     * 1. 클라이언트 요청 → Redis SET에 사용자 추가/제거 (즉시 반환)
     * 2. 스케줄러가 1분마다 Redis의 변경분을 DB에 배치 반영
     *
     * 좋아요 처리 방식 비교:
     * ┌─────────────────┬──────────────────────────────────────────────────┐
     * │ 방식            │ 특징                                              │
     * ├─────────────────┼──────────────────────────────────────────────────┤
     * │ v1 더티체킹     │ ❌ 동시성 이슈 (Lost Update)                     │
     * │                 │ ❌ 매 요청마다 3~5번 DB 쿼리                     │
     * │ v2 Redis+SET    │ ✅ 동시성 안전 (Redis Single-threaded)          │
     * │                 │ ✅ DB 부하 대폭 감소 (배치 동기화)              │
     * │                 │ ✅ 초고속 응답 (~0.1ms)                         │
     * └─────────────────┴──────────────────────────────────────────────────┘
     *
     * Redis 장애 시: DB 직접 처리로 자동 fallback
     *
     * @param boastCatPostId 게시글 ID
     * @param user 인증된 사용자
     * @return 좋아요 상태 (liked: true/false) 및 현재 좋아요 수
     */
    @PostMapping("/v2/{boastCatPostId}")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLikeV2(
            @PathVariable Long boastCatPostId,
            @AuthenticationPrincipal PrincipalUser user) {

        Long userId = user.getUser().getId();
        boolean liked = likeCountService.toggleLike(PostType.BOAST, boastCatPostId, userId);
        Long likeCount = likeCountService.getLikeCount(PostType.BOAST, boastCatPostId);

        String message = liked ? "좋아요 등록 성공" : "좋아요 취소 성공";
        LikeResponse response = new LikeResponse(liked, likeCount);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, message, response));
    }

    /**
     * 좋아요 수 조회 API - Redis 방식 (v2 - 최적화 버전)
     *
     * Redis SET의 SCARD 명령어로 O(1) 시간에 조회합니다.
     *
     * @param boastCatPostId 게시글 ID
     * @return 현재 좋아요 수
     */
    @GetMapping("/v2/{boastCatPostId}")
    public ResponseEntity<ApiResponse<Long>> getLikeCountV2(@PathVariable Long boastCatPostId) {
        Long count = likeCountService.getLikeCount(PostType.BOAST, boastCatPostId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "좋아요 수 조회 성공", count));
    }

    /**
     * 좋아요 여부 확인 API - Redis 방식 (v2 - 최적화 버전)
     *
     * Redis SET의 SISMEMBER 명령어로 O(1) 시간에 확인합니다.
     *
     * @param boastCatPostId 게시글 ID
     * @param user 인증된 사용자
     * @return 좋아요 여부
     */
    @GetMapping("/v2/{boastCatPostId}/status")
    public ResponseEntity<ApiResponse<Boolean>> isLikedV2(
            @PathVariable Long boastCatPostId,
            @AuthenticationPrincipal PrincipalUser user) {

        Long userId = user.getUser().getId();
        boolean liked = likeCountService.isLiked(PostType.BOAST, boastCatPostId, userId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "좋아요 여부 조회 성공", liked));
    }
}
