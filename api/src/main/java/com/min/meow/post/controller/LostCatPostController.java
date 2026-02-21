package com.min.meow.post.controller;


import com.min.meow.global.PostType;
import com.min.meow.global.PrincipalUser;
import com.min.meow.global.PageResponse;
import com.min.meow.global.ApiResponse;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.service.ViewCountService;
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
    private final ViewCountService viewCountService;

    /**
     * 모든 게시물 조회 (Projection 적용)
     *
     * 성능 최적화:
     * - DTO Projection으로 필요한 컬럼만 SELECT
     * - LazyInitializationException 방지
     * - 네트워크 트래픽 감소
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LostCatPostListResponse>>> getAllLostCatPosts(
                                                    @RequestParam (defaultValue = "0") int page,
                                                    @RequestParam (defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<LostCatPostListResponse> pageResponse = lostCatPostServiceImpl.getAllLostCatPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success("모든 글 조회 성공", pageResponse));
    }

    // 글 상세 조회
    @GetMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<GetLostCatPostResponse>> getLostCatPostDetail(@PathVariable Long lostCatPostId){

        GetLostCatPostResponse lostCatPostDto = lostCatPostServiceImpl.getLostCatPost(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("글 조회 성공", lostCatPostDto));
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("글 생성 성공", lostCatPostDto));
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
        return ResponseEntity.ok(ApiResponse.success("글 수정 성공", lostCatPostDto));
    }

    // 글 삭제
    @DeleteMapping("/{lostCatPostId}")
    public ResponseEntity<Void> deleteLostCatPost(@PathVariable Long lostCatPostId, @AuthenticationPrincipal PrincipalUser user){

        String loginId = user.getUser().getLoginId();
        String password = user.getUser().getPassword();
        lostCatPostServiceImpl.deleteLostCatPost(lostCatPostId, loginId, password);
        return ResponseEntity.noContent().build();
    }

    /**
     * 최근 실종글 20개 조회 (DTO Projection 적용)
     *
     * 성능 최적화:
     * - QueryDSL Projection으로 필요한 컬럼만 SELECT
     * - contents, imageUrls 등 불필요한 데이터 제외
     * - Entity 변환 오버헤드 제거
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<LostCatPostListResponse>>> getRecentLostCatPosts() {
        List<LostCatPostListResponse> posts = lostCatPostServiceImpl.getRecentLostCatPosts();
        return ResponseEntity.ok(ApiResponse.success("최근 실종글 20개 조회 성공", posts));
    }

    /**
     * 조회수 증가 API - 원자적 쿼리 방식 (v2 - 개선된 버전)
     *
     * DB 레벨에서 view = view + 1을 수행하여 동시성 문제를 해결합니다.
     * K6 동시성 테스트 후 더티 체킹 방식에서 개선된 버전입니다.
     *
     * 실행되는 쿼리:
     * UPDATE lost_cat_post SET view = view + 1 WHERE id = ?
     */
    @PostMapping("/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(@PathVariable Long lostCatPostId) {
        lostCatPostServiceImpl.incrementViewCount(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공", null));
    }

    /**
     * 조회수 증가 API - 더티 체킹 방식 (v1 - 동시성 이슈 있음)
     *
     * ⚠️ 동시성 문제 (Lost Update):
     * 이 API는 JPA 더티 체킹을 사용하여 조회수를 증가시킵니다.
     * 동시 요청 시 일부 업데이트가 손실되는 Lost Update 문제가 있습니다.
     *
     * K6 동시성 테스트 결과:
     * - 1000 VU 동시 요청 시 약 800~900 정도만 증가 (100~200 손실)
     *
     * 이 문제를 발견하여 v2 (원자적 쿼리 방식)으로 개선하였습니다.
     *
     * @deprecated 동시성 이슈로 인해 POST /{lostCatPostId}/view 사용 권장
     */
    @PostMapping("/v1/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCountV1(@PathVariable Long lostCatPostId) {
        lostCatPostServiceImpl.incrementViewCountWithDirtyChecking(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공 (더티 체킹 방식)", null));
    }

    /**
     * 조회수 증가 API - Redis INCR 방식 (v3 - 최적화 버전)
     *
     * Redis의 INCR 명령어를 사용하여 조회수를 원자적으로 증가시킵니다.
     * DB 부하를 대폭 줄이고, 동시성 문제를 완벽하게 해결합니다.
     *
     * 동작 방식:
     * 1. 클라이언트 요청 → Redis INCR로 조회수 증가 (즉시 반환)
     * 2. 스케줄러가 1분마다 Redis의 증가분을 DB에 배치 반영
     *
     * 조회수 처리 방식 비교:
     * ┌─────────────────┬──────────────────────────────────────────────────┐
     * │ 방식            │ 특징                                              │
     * ├─────────────────┼──────────────────────────────────────────────────┤
     * │ v1 더티체킹     │ ❌ 동시성 이슈 (Lost Update)                     │
     * │ v2 원자적쿼리   │ ✅ 동시성 안전 ⚠️ 매 요청마다 DB UPDATE        │
     * │ v3 Redis+INCR   │ ✅ 동시성 안전 ✅ DB 부하 대폭 감소             │
     * └─────────────────┴──────────────────────────────────────────────────┘
     *
     * Redis 장애 시: DB 직접 업데이트로 자동 fallback
     */
    @PostMapping("/v3/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<Long>> incrementViewCountV3(@PathVariable Long lostCatPostId) {
        Long newCount = viewCountService.incrementViewCount(PostType.LOST, lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공 (Redis INCR 방식)", newCount));
    }
}
