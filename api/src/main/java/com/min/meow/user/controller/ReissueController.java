package com.min.meow.user.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.user.dto.response.TokenResponse;
import com.min.meow.security.service.ReissueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 토큰 재발급 컨트롤러
 * 책임: HTTP 요청/응답 처리 (쿠키 추출, 헤더/쿠키 설정)
 */
@Tag(name = "인증", description = "JWT 토큰 재발급 API")
@RestController
@RequiredArgsConstructor
public class ReissueController {

    private final ReissueService reissueService;

    @Operation(summary = "토큰 재발급",
            description = "Refresh Token(쿠키)으로 새 Access Token을 발급합니다. "
                    + "응답 헤더 Authorization에 새 Access Token, 쿠키에 새 Refresh Token이 설정됩니다. "
                    + "인증 불필요 (Refresh Token은 쿠키로 자동 전송됩니다).")
    @SecurityRequirements  // JWT Bearer 인증 불필요 - 쿠키 기반 Refresh Token 사용
    @PostMapping("/api/auth/token/refresh")
    public ResponseEntity<?> reissue(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response) {

        String refreshToken = extractRefreshToken(request);

        // refreshToken이 없으면 조기 검증하여 서비스 레이어 도달 전에 차단
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        TokenResponse tokenResponse = reissueService.reissue(refreshToken);

        response.setHeader("Authorization", "Bearer " + tokenResponse.getAccessToken());
        response.addCookie(createCookie("refresh", tokenResponse.getRefreshToken()));

        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", null));
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("refresh".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // 쿠키 생성
    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(14 * 24 * 60 * 60); // 14일
        //cookie.setSecure(true); -> https 통신시 필요
        cookie.setPath("/");
        cookie.setHttpOnly(true); // XSS 공격 방어
        return cookie;
    }
}
