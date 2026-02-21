package com.min.meow.user.controller;


import com.min.meow.global.PrincipalUser;
import com.min.meow.global.ApiResponse;
import com.min.meow.user.dto.reponse.JoinResponse;
import com.min.meow.user.dto.reponse.LoginResponse;
import com.min.meow.user.dto.request.JoinRequest;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.user.service.UserService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<JoinResponse>> join(@RequestBody @Valid JoinRequest joinRequest){

        JoinResponse joinResponse = userService.join(joinRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입 성공", joinResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest loginRequest){
        LoginResponse loginResponse = userService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", loginResponse));
    }

    /**
     * - 현재 로그인한 사용자만 자신의 계정을 탈퇴할 수 있음
     * - 소프트 삭제 방식으로 데이터 무결성 유지
     * - 개인정보 비식별화 처리
     * - 탈퇴 후에도 기존 게시글은 "탈퇴한 사용자"로 표시됨
     * @param user 현재 로그인한 사용자 정보
     * @return 탈퇴 완료 메시지
     */
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal PrincipalUser user) {
        userService.withdraw(user.getLoginId());
        return ResponseEntity.noContent().build();
    }

}
