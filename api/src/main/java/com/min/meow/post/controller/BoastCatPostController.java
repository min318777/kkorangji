package com.min.meow.post.controller;

import com.min.meow.common.PostType;
import com.min.meow.common.PrincipalUser;
import com.min.meow.common.PageResponse;
import com.min.meow.common.ApiResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import com.min.meow.post.service.ViewCountService;
import com.min.meow.post.service.BoastCatPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Tag(name = "자랑글", description = "고양이 자랑 게시글 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow/boast-cat")
public class BoastCatPostController {
    private final BoastCatPostService boastCatPostService;
    private final ViewCountService viewCountService;

    // ========== CRUD ==========

    @Operation(summary = "자랑글 목록 조회", description = "페이징된 자랑글 목록을 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BoastCatPostListResponse>>> getAllBoastCatPost(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam (defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam (defaultValue = "10") int size ){

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<BoastCatPostListResponse> posts = boastCatPostService.getAllBoastCatPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success("모든 글 조회 성공", posts));
    }

    @Operation(summary = "자랑글 상세 조회", description = "게시글 ID로 상세 정보를 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<GetBoastCatPostResponse>> getBoastPostId(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId){
        GetBoastCatPostResponse getBoastCatPostResponse = boastCatPostService.getBoastCatPost(boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("글 조회 성공", getBoastCatPostResponse));
    }

    /**
     * 글 생성 (Presigned URL 기반 이미지 업로드)
     * 이미지 업로드 플로우:
     * 1. 클라이언트가 /api/images/presigned-urls 로 Presigned URL 요청
     * 2. 클라이언트가 Presigned URL로 S3에 이미지 직접 업로드
     * 3. 업로드 완료 후 받은 S3 key를 imageKeys에 담아서 이 API 호출
     */
    @Operation(summary = "자랑글 생성",
            description = "새 자랑글을 작성합니다. 이미지는 Presigned URL로 S3에 먼저 업로드 후 key를 전달합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('post:write')")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateBoastCatPostResponse>> createBoastCatPost(
            @RequestBody @Valid CreateBoastCatPostRequest createBoastCatPostRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){
        CreateBoastCatPostResponse post = boastCatPostService.createBoastCatPost(createBoastCatPostRequest, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("글 생성 성공", post));
    }

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     * 이미지 처리:
     * - newImageKeys: 새로 업로드된 이미지의 S3 key
     * - keepImageUrls: 유지할 기존 이미지의 CloudFront URL
     * - deleteImageUrls: 삭제할 이미지의 CloudFront URL
     */
    @Operation(summary = "자랑글 수정",
            description = "자랑글을 수정합니다. 본인 게시글만 수정 가능합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('post:write')")
    @PutMapping("/{boastCatPostId}")
    public ResponseEntity<ApiResponse<UpdateBoastCatPostResponse>> updateBoastCatPost(
            @RequestBody @Valid UpdateBoastCatPostRequest updateBoastCatPostRequest,
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        Long userId = user.getUserId();
        UpdateBoastCatPostResponse post = boastCatPostService.updateBoastCatPost(updateBoastCatPostRequest, boastCatPostId, userId);
        return ResponseEntity.ok(ApiResponse.success("글 수정 성공", post));
    }

    @Operation(summary = "자랑글 삭제",
            description = "자랑글을 삭제합니다. 본인 게시글만 삭제 가능합니다. 인증 필요.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{boastCatPostId}")
    public ResponseEntity<Void> deleteBoastCatPost(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        Long userId = user.getUserId();
        boastCatPostService.deleteBoastCatPost(boastCatPostId, userId);
        return ResponseEntity.noContent().build();
    }

    // ========== 조회수 ==========

    /**
     * 조회수 증가 API - 더티 체킹 방식 (v1 - 동시성 이슈 있음)
     * @deprecated 동시성 이슈로 인해 POST /{boastCatPostId}/view 사용 권장
     */
    @Deprecated
    @Operation(summary = "조회수 증가 (v1 더티체킹)",
            description = "JPA 더티 체킹 방식. 동시성 이슈(Lost Update)가 있으므로 v2 사용을 권장합니다.",
            deprecated = true)
    @SecurityRequirements
    @PostMapping("/v1/{boastCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCountV1(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId) {
        boastCatPostService.incrementViewCountWithDirtyChecking(boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공 (더티 체킹 방식)", null));
    }

    /**
     * 조회수 증가 API - 원자적 쿼리 방식 (v2 - 개선된 버전)
     * DB 레벨에서 view = view + 1을 수행하여 동시성 문제를 해결합니다.
     * K6 동시성 테스트 후 더티 체킹 방식에서 개선된 버전입니다.
     * 실행되는 쿼리:
     * UPDATE boast_cat_post SET view = view + 1 WHERE id = ?
     */
    @Operation(summary = "조회수 증가 (v2 원자적)",
            description = "DB 원자적 쿼리로 조회수를 증가시킵니다. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/{boastCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId) {
        boastCatPostService.incrementViewCount(boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공", null));
    }

    /**
     * 조회수 증가 API - Redis INCR 방식 (v3 - 최적화 버전)
     * Redis의 INCR 명령어를 사용하여 조회수를 원자적으로 증가시킵니다.
     * DB 부하를 대폭 줄이고, 동시성 문제를 완벽하게 해결합니다.
     * 동작 방식:
     * 1. 클라이언트 요청 → Redis INCR로 조회수 증가 (즉시 반환)
     * 2. 스케줄러가 1분마다 Redis의 증가분을 DB에 배치 반영
     * Redis 장애 시: DB 직접 업데이트로 자동 fallback
     */
    @Operation(summary = "조회수 증가 (v3 Redis)",
            description = "Redis INCR 방식. DB 부하를 줄이고 동시성을 완벽 보장합니다. Redis 장애 시 DB fallback. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/v3/{boastCatPostId}/view")
    public ResponseEntity<ApiResponse<Long>> incrementViewCountV3(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId,
            HttpServletRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        String identifier = (user != null)
                ? "user:" + user.getUserId()
                : "ip:" + getClientIp(request);

        Long newCount = viewCountService.incrementViewCount(PostType.BOAST, boastCatPostId, identifier);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공 (Redis INCR 방식)", newCount));
    }

    /**
     * 조회수 증가 API - 비관적 락 방식 (v4)
     * SELECT FOR UPDATE로 행을 잠근 뒤 더티 체킹으로 조회수를 증가시킵니다.
     * v1(더티 체킹)과 달리 락으로 순서를 강제하여 Lost Update를 방지합니다.
     * 비교 대상:
     * - v1(더티 체킹): 락 없음 → Lost Update 발생
     * - v2(원자적): DB가 알아서 처리 → 락 유지 시간 최소
     * - v4(비관적 락): SELECT ~ 커밋까지 락 유지 → 정합성 보장, 처리량 낮음
     */
    @Operation(summary = "조회수 증가 (v4 비관적 락)",
            description = "SELECT FOR UPDATE + 더티 체킹 방식. 정합성 보장, 대용량 트래픽 시 처리량 낮음. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/v4/{boastCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCountV4(
            @Parameter(description = "자랑글 ID", example = "1")
            @PathVariable Long boastCatPostId) {
        boastCatPostService.incrementViewCountWithPessimisticLock(boastCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공 (비관적 락 방식)", null));
    }

    // X-Forwarded-For 헤더 우선, 없으면 RemoteAddr
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}