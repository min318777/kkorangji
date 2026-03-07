package com.min.meow.security.controller;

import com.min.meow.global.ApiResponse;
import com.min.meow.global.PrincipalUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JwtFilter v1 vs v2 성능 비교를 위한 경량 테스트 컨트롤러.
 * <p>DB 쿼리 없이 인증만 확인하고 즉시 응답한다.
 * 서비스 레이어의 DB 조회를 제거하여 순수 JwtFilter 성능 차이를 측정.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthTestController {

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> authTest(
            @AuthenticationPrincipal PrincipalUser user) {
        return ResponseEntity.ok(
                ApiResponse.success("인증 성공", user.getLoginId()));
    }
}
