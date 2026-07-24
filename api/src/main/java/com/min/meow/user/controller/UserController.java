package com.min.meow.user.controller;


import com.min.meow.common.PrincipalUser;
import com.min.meow.common.ApiResponse;
import com.min.meow.user.dto.response.JoinResponse;
import com.min.meow.user.dto.response.LoginResponse;
import com.min.meow.user.dto.request.JoinRequest;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "회원가입, 로그인, 탈퇴 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입",
            description = "새 사용자를 등록합니다. 아이디 중복 시 409 응답. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<JoinResponse>> join(@RequestBody @Valid JoinRequest joinRequest){

        JoinResponse joinResponse = userService.join(joinRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입 성공", joinResponse));
    }

    @Operation(summary = "로그인 (서비스 직접 호출)",
            description = "아이디/비밀번호로 로그인합니다. 실제 인증은 POST /api/users/login (CustomLoginFilter)이 처리합니다. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/login/form")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest loginRequest){
        LoginResponse loginResponse = userService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", loginResponse));
    }

    /**
     * - 현재 로그인한 사용자만 자신의 계정을 탈퇴할 수 있음
     * - 소프트 삭제 방식으로 데이터 무결성 유지
     * - 개인정보 비식별화 처리
     * - 탈퇴 후에도 기존 게시글은 "탈퇴한 사용자"로 표시됨
     */
    @Operation(summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 탈퇴합니다. 소프트 삭제 방식으로 개인정보는 비식별화됩니다. 인증 필요.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {
        userService.withdraw(user.getUserId());
        return ResponseEntity.noContent().build();
    }

}
