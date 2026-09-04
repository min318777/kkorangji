package com.min.meow.post.controller;


import com.min.meow.common.PrincipalUser;
import com.min.meow.common.PageResponse;
import com.min.meow.common.ApiResponse;
import com.min.meow.common.SecurityUtil;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.service.LostCatPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "실종글", description = "실종 고양이 신고 게시글 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow/lost-cat-posts")
public class LostCatPostController {

    private final LostCatPostService lostCatPostService;

    // ========== CRUD ==========

    @Operation(summary = "실종글 목록 조회", description = "페이징된 실종글 목록을 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LostCatPostListResponse>>> getAllLostCatPosts(
            @PageableDefault(size = 10) Pageable pageable) {

        PageResponse<LostCatPostListResponse> pageResponse = lostCatPostService.getAllLostCatPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success("모든 글 조회 성공", pageResponse));
    }

    @Operation(summary = "실종글 상세 조회", description = "게시글 ID로 상세 정보를 조회합니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<GetLostCatPostResponse>> getLostCatPostDetail(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId){

        GetLostCatPostResponse lostCatPostDto = lostCatPostService.getLostCatPost(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("글 조회 성공", lostCatPostDto));
    }

    /**
     * 글 생성 (Presigned URL 기반 이미지 업로드)
     */
    @Operation(summary = "실종글 생성",
            description = "새 실종글을 작성합니다. 이미지는 Presigned URL로 S3에 먼저 업로드 후 key를 전달합니다. 인증 필요.")
    @PreAuthorize("hasAuthority('post:create')")
    @PostMapping
    public ResponseEntity<ApiResponse<CreateLostCatPostResponse>> createLostCatPost(
            @RequestBody @Valid CreateLostCatPostRequest createLostCatPostRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        CreateLostCatPostResponse lostCatPostDto = lostCatPostService.createLostCatPost(createLostCatPostRequest, user.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("글 생성 성공", lostCatPostDto));
    }

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     */
    @Operation(summary = "실종글 수정",
            description = "실종글을 수정합니다. 본인 게시글만 수정 가능합니다(관리자는 타인 게시글도 수정 가능). 인증 필요.")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<UpdateLostCatPostResponse>> updateLostCatPost(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            @RequestBody @Valid UpdateLostCatPostRequest updateLostCatPostRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){
        Long userId = user.getUserId();
        UpdateLostCatPostResponse lostCatPostDto = lostCatPostService.updateLostCatPost(lostCatPostId, updateLostCatPostRequest, userId);
        return ResponseEntity.ok(ApiResponse.success("글 수정 성공", lostCatPostDto));
    }

    @Operation(summary = "실종글 삭제",
            description = "실종글을 삭제합니다. 본인 게시글만 삭제 가능합니다(관리자는 타인 게시글도 삭제 가능). 인증 필요.")
    @PreAuthorize("hasAuthority('post:delete')")
    @DeleteMapping("/{lostCatPostId}")
    public ResponseEntity<Void> deleteLostCatPost(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user){

        Long userId = user.getUserId();
        boolean hasDeleteAuthority = SecurityUtil.hasAuthority("post:delete:any");
        lostCatPostService.deleteLostCatPost(lostCatPostId, userId, hasDeleteAuthority);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "실종 상태 변경",
            description = "찾는 중 ↔ 귀가 완료 상태를 변경합니다. 본인 게시글만 변경 가능. 인증 필요.")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{lostCatPostId}/status")
    public ResponseEntity<ApiResponse<Void>> updateCompletedStatus(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            @RequestBody Map<String, Boolean> body,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {
        boolean isCompleted = Boolean.TRUE.equals(body.get("isCompleted"));
        lostCatPostService.updateCompletedStatus(lostCatPostId, isCompleted, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("상태 변경 성공", null));
    }

    // ========== 위치 기반 조회 ==========

    @Operation(summary = "내 주변 실종글 조회",
            description = "현재 위치(위도/경도) 기준으로 반경 내 실종글을 조회합니다. 위치 없는 글은 제외됩니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<PageResponse<LostCatPostListResponse>>> getNearbyLostCatPosts(
            @Parameter(description = "현재 위치 위도", example = "37.5665") @RequestParam double lat,
            @Parameter(description = "현재 위치 경도", example = "126.9780") @RequestParam double lng,
            @Parameter(description = "검색 반경 (km, 기본값 5)", example = "5") @RequestParam(defaultValue = "5") double radius,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<LostCatPostListResponse> pageResponse = lostCatPostService.getNearbyLostCatPosts(lat, lng, radius, pageable);
        return ResponseEntity.ok(ApiResponse.success("내 주변 실종글 조회 성공", pageResponse));
    }

    @Operation(summary = "내 주변 실종글 조회 (ST_Distance_Sphere)",
            description = "ST_Distance_Sphere로 정확한 원형 반경 필터 + 가까운 순 정렬. 성능 비교용. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/nearby/st")
    public ResponseEntity<ApiResponse<PageResponse<LostCatPostListResponse>>> getNearbyLostCatPostsST(
            @Parameter(description = "현재 위치 위도", example = "37.5665") @RequestParam double lat,
            @Parameter(description = "현재 위치 경도", example = "126.9780") @RequestParam double lng,
            @Parameter(description = "검색 반경 (km, 기본값 5)", example = "5") @RequestParam(defaultValue = "5") double radius,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PageResponse<LostCatPostListResponse> pageResponse = lostCatPostService.getNearbyLostCatPostsST(lat, lng, radius, pageable);
        return ResponseEntity.ok(ApiResponse.success("내 주변 실종글 조회 성공 (ST)", pageResponse));
    }

    // ========== 조회수 ==========

    /**
     * @deprecated 동시성 이슈로 인해 POST /{lostCatPostId}/view 사용 권장
     */
    @Deprecated
    @Operation(summary = "조회수 증가 (v1 더티체킹)",
            description = "JPA 더티 체킹 방식. 동시성 이슈(Lost Update)가 있으므로 v2 사용을 권장합니다.",
            deprecated = true)
    @SecurityRequirements
    @PostMapping("/v1/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCountV1(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId) {
        lostCatPostService.incrementViewCountWithDirtyChecking(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공 (더티 체킹 방식)", null));
    }

    @Operation(summary = "조회수 증가 (v2 원자적)",
            description = "DB 원자적 쿼리로 조회수를 증가시킵니다. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<Void>> incrementViewCount(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId) {
        lostCatPostService.incrementViewCount(lostCatPostId);
        return ResponseEntity.ok(ApiResponse.success("조회수 증가 성공", null));
    }

    @Operation(summary = "실종글 상세조회 + 조회수 증가 통합 (v3 Redis INCR)",
            description = "Redis INCR 방식. DB 부하를 줄이고 동시성을 완벽 보장합니다. Redis 장애 시 DB fallback. " +
                    "조회수는 배치 동기화(30초 주기) 전까지 DB 값 그대로 응답에 실립니다. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/v3/{lostCatPostId}/view")
    public ResponseEntity<ApiResponse<GetLostCatPostResponse>> incrementViewCountV3(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            HttpServletRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        String identifier = (user != null)
                ? "user:" + user.getUserId()
                : "ip:" + getClientIp(request);

        GetLostCatPostResponse response = lostCatPostService.getLostCatPostV3(lostCatPostId, identifier);
        return ResponseEntity.ok(ApiResponse.success("상세조회 성공 (v3)", response));
    }

    @Operation(summary = "실종글 상세조회 + 조회수 증가 통합 (v3 Redis INCR, GET)",
            description = "자랑글과 동일한 GET 방식의 상세조회+조회수증가 통합 API입니다. Redis INCR 후 상세조회를 반환합니다. " +
                    "기존 POST /v3/{lostCatPostId}/view는 하위 호환을 위해 유지됩니다. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/view/v3/{lostCatPostId}")
    public ResponseEntity<ApiResponse<GetLostCatPostResponse>> getLostCatPostV3(
            @Parameter(description = "실종글 ID", example = "1")
            @PathVariable Long lostCatPostId,
            HttpServletRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        String identifier = (user != null)
                ? "user:" + user.getUserId()
                : "ip:" + getClientIp(request);

        GetLostCatPostResponse response = lostCatPostService.getLostCatPostV3(lostCatPostId, identifier);
        return ResponseEntity.ok(ApiResponse.success("상세조회 성공 (v3)", response));
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
