package com.min.meow.user.controller;

import com.min.meow.global.PrincipalUser;
import com.min.meow.global.PostType;
import com.min.meow.global.ApiResponse;
import com.min.meow.user.dto.reponse.MyCommentListResponse;
import com.min.meow.user.dto.reponse.MyPageSummaryResponse;
import com.min.meow.user.dto.reponse.MyPostListResponse;
import com.min.meow.user.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/mypage")
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * 마이페이지
     * GET /api/users/mypage
     * @param user 현재 인증된 사용자 정보 (Spring Security Context에서 자동 주입)
     * @return 사용자 기본 정보 및 통계 (작성한 글 수, 댓글 수)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageSummaryResponse>> getMyPageSummary(
            @AuthenticationPrincipal PrincipalUser user) {

        String loginId = user.getLoginId();
        MyPageSummaryResponse response = myPageService.getMyPageSummary(loginId);

        return ResponseEntity.ok(
                new ApiResponse<>(HttpStatus.OK, "마이페이지 조회 성공", response)
        );
    }

    /**
     * 내가 쓴 글 목록 조회
     * GET /api/users/mypage/posts
     * @param user 현재 인증된 사용자 정보 (Spring Security Context에서 자동 주입)
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param type 게시글 타입 (ALL: 전체, BOAST: 자랑글, LOST: 실종글, 기본값: ALL)
     * @return 내가 쓴 게시글 목록 (페이징)
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<MyPostListResponse>> getMyPosts(
            @AuthenticationPrincipal PrincipalUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") PostType type) {

        // SecurityContext에서 loginId 추출
        String loginId = user.getLoginId();
        Pageable pageable = PageRequest.of(page, size);
        MyPostListResponse response = myPageService.getMyPosts(loginId, pageable, type);

        return ResponseEntity.ok(
                new ApiResponse<>(HttpStatus.OK, "내가 쓴 글 조회 성공", response)
        );
    }

    /**
     * 내가 쓴 댓글 목록 조회
     * GET /api/users/mypage/comments
     * @param user 현재 인증된 사용자 정보 (Spring Security Context에서 자동 주입)
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return 내가 쓴 댓글 목록 (페이징)
     */
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<MyCommentListResponse>> getMyComments(
            @AuthenticationPrincipal PrincipalUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = user.getUser().getId();
        Pageable pageable = PageRequest.of(page, size);
        MyCommentListResponse response = myPageService.getMyComments(userId, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(HttpStatus.OK, "내가 쓴 댓글 조회 성공", response)
        );
    }
}