package com.min.meow.user.controller;


import com.min.meow.common.ApiResponse;
import com.min.meow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 소셜 로그인 provider가 호출하는 웹훅 전용 컨트롤러
 * 사용자 인증 없이 provider 서버가 직접 호출하므로 인증 불필요 (SecurityConfig permitAll)
 */
@Tag(name = "인증 웹훅", description = "소셜 로그인 provider가 호출하는 웹훅")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthWebhookController {

    private final UserService userService;

    @Operation(summary = "카카오 연결 해제 웹훅",
            description = "사용자가 카카오 계정 설정에서 서비스 연결을 끊으면 카카오가 호출. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/kakao/webhook/unlink")
    public ResponseEntity<ApiResponse<Void>> kakaoUnlinkWebhook(
            @RequestParam(name = "user_id") String userId) {
        log.info("카카오 연결 해제 웹훅 수신 - user_id: {}", userId);
        userService.withdrawByKakaoUnlink(userId);
        return ResponseEntity.ok(ApiResponse.success("처리 완료", null));
    }
}
