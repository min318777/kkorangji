package com.min.meow.post.controller;

import com.min.meow.config.PrincipalUser;
import com.min.meow.global.PageResponse;
import com.min.meow.global.exception.ApiResponse;
import com.min.meow.post.domain.request.CreateBoastCatPostRequest;
import com.min.meow.post.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.post.domain.response.CreateBoastCatPostResponse;
import com.min.meow.post.domain.response.GetBoastCatPostResponse;
import com.min.meow.post.domain.response.UpdateBoastCatPostResponse;
import com.min.meow.post.service.impl.BoastCatPostServiceImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/meow/boast-cat")
public class BoastCatPostController {
    private final BoastCatPostServiceImpl boastCatPostServiceImpl;

    // 모든 글 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<GetBoastCatPostResponse>>> getAllBoastCatPost(
                                                        @RequestParam (defaultValue = "0") int page,
                                                        @RequestParam (defaultValue = "10") int size ){

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<GetBoastCatPostResponse> posts = boastCatPostServiceImpl.getAllBoastCatPosts(pageable);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "모든 글 조회 성공", posts));
    }

    // 글 상세 조회
    @GetMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getBoastPostId(@PathVariable Long boastCatPostId){
        GetBoastCatPostResponse getBoastCatPostResponse = boastCatPostServiceImpl.getBoastCatPost(boastCatPostId);
        return ResponseEntity.ok(new ApiResponse<GetBoastCatPostResponse>(HttpStatus.OK, "글 조회 성공", getBoastCatPostResponse));
    }

    // 글 생성
    @PostMapping
    public ResponseEntity<ApiResponse<CreateBoastCatPostResponse>> createBoastCatPost(@ModelAttribute CreateBoastCatPostRequest createBoastCatPostRequest,
                                                                                      @AuthenticationPrincipal PrincipalUser user){
        CreateBoastCatPostResponse post = boastCatPostServiceImpl.createBoastCatPost(createBoastCatPostRequest, user.getUser().getLoginId());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 생성 성공", post));
    }

    // 글 수정
    @PutMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<UpdateBoastCatPostResponse>> updateBoastCatPost(@ModelAttribute @Valid UpdateBoastCatPostRequest updateBoastCatPostRequest,
                                                                                      @PathVariable Long boastCatPostId, @AuthenticationPrincipal PrincipalUser user){

        String loginId = user.getUser().getLoginId();
        UpdateBoastCatPostResponse post = boastCatPostServiceImpl.updateBoastCatPost(updateBoastCatPostRequest, boastCatPostId, loginId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 수정 성공", post));
    }

    // 글 삭제
    @DeleteMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<Void>> deleteBoastCatPost(@PathVariable Long boastCatPostId, @AuthenticationPrincipal PrincipalUser user){

        String loginId = user.getUser().getLoginId();
        String password = user.getUser().getPassword();
        boastCatPostServiceImpl.deleteBoastCatPost(boastCatPostId, loginId, password);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 삭제 성공", null));
    }
}
