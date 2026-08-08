package com.min.meow.user.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.common.PrincipalUser;
import com.min.meow.user.dto.response.AdminUserResponse;
import com.min.meow.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "관리자 - 사용자 관리", description = "관리자 전용 사용자 권한 제한/복원/강제탈퇴 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "유저 목록 조회",
            description = "역할 필터(ROLE_USER / ROLE_RESTRICTED / ROLE_ADMIN)와 페이징을 지원합니다. roleName 미입력 시 전체 조회.")
    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUserList(
            @Parameter(description = "역할 필터 (없으면 전체)") @RequestParam(required = false) String roleName,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AdminUserResponse> result = adminUserService.getUserList(roleName, pageable);
        return ResponseEntity.ok(ApiResponse.success("유저 목록 조회 성공", result));
    }

    @Operation(summary = "사용자 상태 변경",
            description = "사용자 상태를 변경합니다. Body: { \"status\": \"RESTRICTED\" | \"ACTIVE\" }. "
                    + "RESTRICTED: 조회만 가능, 작성·수정·댓글 즉시 차단. "
                    + "ACTIVE: 모든 일반 권한 즉시 복구.")
    @PreAuthorize("hasAuthority('user:restrict')")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser admin,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        AdminUserResponse response = switch (status) {
            case "RESTRICTED" -> adminUserService.restrictUser(admin.getUserId(), userId);
            case "ACTIVE"     -> adminUserService.restoreUser(admin.getUserId(), userId);
            default -> throw new IllegalArgumentException("지원하지 않는 상태값입니다: " + status + " (RESTRICTED | ACTIVE)");
        };
        return ResponseEntity.ok(ApiResponse.success("사용자 상태 변경 완료", response));
    }

    @Operation(summary = "강제 탈퇴",
            description = "대상 사용자를 강제 탈퇴 처리합니다. 개인정보가 비식별화되고 모든 디바이스에서 즉시 로그아웃됩니다.")
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> forceWithdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser admin,
            @PathVariable Long userId) {

        adminUserService.forceWithdraw(admin.getUserId(), userId);
        return ResponseEntity.noContent().build();
    }
}
