package com.min.meow.user.controller;

import com.min.meow.common.PageResponse;
import com.min.meow.common.PrincipalUser;
import com.min.meow.common.PostType;
import com.min.meow.common.ApiResponse;
import com.min.meow.user.dto.response.MyCommentDto;
import com.min.meow.user.dto.response.MyPageSummaryResponse;
import com.min.meow.user.dto.response.MyPostDto;
import com.min.meow.user.dto.request.UpdateProfileRequest;
import com.min.meow.user.service.MyPageService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지", description = "사용자 마이페이지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(summary = "마이페이지 요약 조회",
            description = "사용자 기본 정보 및 통계(작성 글 수, 댓글 수)를 조회합니다. 인증 필요.")
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageSummaryResponse>> getMyPageSummary(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        Long userId = user.getUserId();
        MyPageSummaryResponse response = myPageService.getMyPageSummary(userId);

        return ResponseEntity.ok(
                ApiResponse.success( "마이페이지 조회 성공", response)
        );
    }

    @Operation(summary = "프로필 수정",
            description = "닉네임을 수정합니다. 인증 필요.")
    @PatchMapping
    public ResponseEntity<ApiResponse<MyPageSummaryResponse>> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user,
            @Valid @RequestBody UpdateProfileRequest request) {

        Long userId = user.getUserId();
        MyPageSummaryResponse response = myPageService.updateProfile(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("프로필이 수정되었습니다.", response)
        );
    }

    @Operation(summary = "내가 쓴 글 목록 조회",
            description = "현재 사용자가 작성한 게시글 목록을 페이징으로 조회합니다. type으로 게시글 종류를 필터링할 수 있습니다. 인증 필요.")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<MyPostDto>>> getMyPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user,
            @Parameter(description = "게시글 타입 필터", example = "BOAST",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"BOAST", "LOST"}))
            @RequestParam PostType type,
            @PageableDefault(size = 10) Pageable pageable) {

        Long userId = user.getUserId();
        PageResponse<MyPostDto> response = myPageService.getMyPosts(userId, pageable, type);

        return ResponseEntity.ok(
                ApiResponse.success( "내가 쓴 글 조회 성공", response)
        );
    }

    @Operation(summary = "내가 쓴 댓글 목록 조회",
            description = "현재 사용자가 작성한 댓글 목록을 페이징으로 조회합니다. 인증 필요.")
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<PageResponse<MyCommentDto>>> getMyComments(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user,
            @PageableDefault(size = 10) Pageable pageable) {

        Long userId = user.getUserId();
        PageResponse<MyCommentDto> response = myPageService.getMyComments(userId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success( "내가 쓴 댓글 조회 성공", response)
        );
    }

    @Operation(summary = "내가 좋아요한 글 목록 조회",
            description = "현재 사용자가 좋아요한 자랑글 목록을 페이징으로 조회합니다. 인증 필요.")
    @GetMapping("/liked-posts")
    public ResponseEntity<ApiResponse<PageResponse<MyPostDto>>> getMyLikedPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user,
            @PageableDefault(size = 10) Pageable pageable) {

        PageResponse<MyPostDto> response = myPageService.getMyLikedPosts(user.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("좋아요한 글 조회 성공", response));
    }
}
