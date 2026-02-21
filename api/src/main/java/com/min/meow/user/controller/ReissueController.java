package com.min.meow.user.controller;

import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.dto.response.TokenResponse;
import com.min.meow.user.service.ReissueService;
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
@RestController
@RequiredArgsConstructor
public class ReissueController {

    private final ReissueService reissueService;

    @PostMapping("/api/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = extractRefreshToken(request);

        // refreshToken이 없으면 조기 검증하여 서비스 레이어 도달 전에 차단
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        TokenResponse tokenResponse = reissueService.reissue(refreshToken);

        response.setHeader("Authorization", "Bearer " + tokenResponse.getAccessToken());
        response.addCookie(createCookie("refresh", tokenResponse.getRefreshToken()));

        return new ResponseEntity<>(HttpStatus.OK);
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
