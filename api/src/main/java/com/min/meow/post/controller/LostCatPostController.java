package com.min.meow.post.controller;


import com.min.meow.global.PrincipalUser;
import com.min.meow.global.PageResponse;
import com.min.meow.global.ApiResponse;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.service.impl.LostCatPostServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow/lost-cat")
public class LostCatPostController {

    private final LostCatPostServiceImpl lostCatPostServiceImpl;

    // 모든 게시물 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<GetLostCatPostResponse>>> getAllLostCatPosts(
                                                    @RequestParam (defaultValue = "0") int page,
                                                    @RequestParam (defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<GetLostCatPostResponse> pageResponse = lostCatPostServiceImpl.getAllLostCatPosts(pageable);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "모든 글 조회 성공", pageResponse));

        //n+1 해결->페치 조인 하지만 페이징안됨
        //return ResponseEntity.ok(new ResponseDto<>(true, "모든 글 조회 성공", lostCatRepository.findAllFetch(pageable).stream().map(UpdateLostCatPostDto::convertToDto)));
    }

    // 글 상세 조회
    @GetMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<GetLostCatPostResponse>> getLostCatPostDetail(@PathVariable Long lostCatPostId){

        GetLostCatPostResponse lostCatPostDto = lostCatPostServiceImpl.getLostCatPost(lostCatPostId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 조회 성공", lostCatPostDto));
    }

    /**
     * 글 생성 (Presigned URL 기반 이미지 업로드)
     * 이미지 업로드 플로우:
     * 1. 클라이언트가 /api/images/presigned-urls 로 Presigned URL 요청
     * 2. 클라이언트가 Presigned URL로 S3에 이미지 직접 업로드
     * 3. 업로드 완료 후 받은 S3 key를 imageKeys에 담아서 이 API 호출
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateLostCatPostResponse>> createLostCatPost(
            @RequestBody @Valid CreateLostCatPostRequest createLostCatPostRequest,
            @AuthenticationPrincipal PrincipalUser user){

        CreateLostCatPostResponse lostCatPostDto = lostCatPostServiceImpl.createLostCatPost(createLostCatPostRequest, user.getUser().getLoginId());

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 생성 성공", lostCatPostDto));
    }

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     * 이미지 처리:
     * - newImageKeys: 새로 업로드된 이미지의 S3 key
     * - keepImageUrls: 유지할 기존 이미지의 CloudFront URL
     * - deleteImageUrls: 삭제할 이미지의 CloudFront URL
     */
    @PutMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<UpdateLostCatPostResponse>> updateLostCatPost(
            @PathVariable Long lostCatPostId,
            @RequestBody @Valid UpdateLostCatPostRequest updateLostCatPostRequest,
            @AuthenticationPrincipal PrincipalUser user){
        String loginId = user.getUser().getLoginId();
        UpdateLostCatPostResponse lostCatPostDto = lostCatPostServiceImpl.updateLostCatPost(lostCatPostId, updateLostCatPostRequest, loginId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 수정 성공", lostCatPostDto));
    }

    // 글 삭제
    @DeleteMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<Void>> deleteLostCatPost(@PathVariable Long lostCatPostId, @AuthenticationPrincipal PrincipalUser user){

        String loginId = user.getUser().getLoginId();
        String password = user.getUser().getPassword();
        lostCatPostServiceImpl.deleteLostCatPost(lostCatPostId, loginId, password);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 삭제 성공", null));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<GetLostCatPostResponse>>> getRecentLostCatPosts() {
        List<GetLostCatPostResponse> posts = lostCatPostServiceImpl.getRecentLostCatPosts();
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "최근 실종글 20개 조회 성공", posts));
    }

    // 조회수 증가 API (별도 분리)
    @PostMapping("/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(@PathVariable Long lostCatPostId) {
        lostCatPostServiceImpl.incrementViewCount(lostCatPostId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "조회수 증가 성공", null));
    }
}
