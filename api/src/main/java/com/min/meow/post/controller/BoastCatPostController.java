package com.min.meow.post.controller;

import com.min.meow.global.PrincipalUser;
import com.min.meow.global.PageResponse;
import com.min.meow.global.ApiResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import com.min.meow.post.service.impl.BoastCatPostServiceImpl;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/meow/boast-cat")
public class BoastCatPostController {
    private final BoastCatPostServiceImpl boastCatPostServiceImpl;

    // 모든 글 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BoastCatPostListResponse>>> getAllBoastCatPost(
                                                        @RequestParam (defaultValue = "0") int page,
                                                        @RequestParam (defaultValue = "10") int size ){

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<BoastCatPostListResponse> posts = boastCatPostServiceImpl.getAllBoastCatPosts(pageable);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "모든 글 조회 성공", posts));
    }


    // 글 상세 조회
    @GetMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getBoastPostId(@PathVariable Long boastCatPostId){
        GetBoastCatPostResponse getBoastCatPostResponse = boastCatPostServiceImpl.getBoastCatPost(boastCatPostId);
        return ResponseEntity.ok(new ApiResponse<GetBoastCatPostResponse>(HttpStatus.OK, "글 조회 성공", getBoastCatPostResponse));
    }

    /**
     * 글 생성 (Presigned URL 기반 이미지 업로드)
     * 이미지 업로드 플로우:
     * 1. 클라이언트가 /api/images/presigned-urls 로 Presigned URL 요청
     * 2. 클라이언트가 Presigned URL로 S3에 이미지 직접 업로드
     * 3. 업로드 완료 후 받은 S3 key를 imageKeys에 담아서 이 API 호출
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateBoastCatPostResponse>> createBoastCatPost(
            @RequestBody @Valid CreateBoastCatPostRequest createBoastCatPostRequest,
            @AuthenticationPrincipal PrincipalUser user){
        CreateBoastCatPostResponse post = boastCatPostServiceImpl.createBoastCatPost(createBoastCatPostRequest, user.getUser().getLoginId());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 생성 성공", post));
    }

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     * 이미지 처리:
     * - newImageKeys: 새로 업로드된 이미지의 S3 key
     * - keepImageUrls: 유지할 기존 이미지의 CloudFront URL
     * - deleteImageUrls: 삭제할 이미지의 CloudFront URL
     */
    @PutMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<UpdateBoastCatPostResponse>> updateBoastCatPost(
            @RequestBody @Valid UpdateBoastCatPostRequest updateBoastCatPostRequest,
            @PathVariable Long boastCatPostId,
            @AuthenticationPrincipal PrincipalUser user){

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

    /**
     * 최근 자랑글 20개 조회 (DTO Projection 적용)
     *
     * 성능 최적화:
     * - QueryDSL Projection으로 필요한 컬럼만 SELECT
     * - contents, imageUrls 등 불필요한 데이터 제외
     * - Entity 변환 오버헤드 제거
     * - BoastCatPostListResponse를 재사용하여 코드 중복 제거
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<BoastCatPostListResponse>>> getRecentBoastCatPosts() {
        List<BoastCatPostListResponse> posts = boastCatPostServiceImpl.getRecentBoastCatPosts();
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "최근 자랑글 20개 조회 성공", posts));
    }

    // 조회수 증가
    @PostMapping("/{boastCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(@PathVariable Long boastCatPostId) {
        boastCatPostServiceImpl.incrementViewCount(boastCatPostId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "조회수 증가 성공", null));
    }
}
