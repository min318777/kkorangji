package com.min.meow.security;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 쿠키 생성을 담당한다.
 * cookie.secure는 local에서는 false(HTTP 개발 환경), prod에서는 true(HTTPS 강제)로 프로파일별로 다르게 설정된다.
 */
@Component
public class RefreshCookieProvider {

    private static final String REFRESH_COOKIE_NAME = "refresh";

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    public Cookie create(String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, value);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // XSS 공격 방어: 자바스크립트로 쿠키 접근 불가
        cookie.setAttribute("SameSite", "Lax"); // CSRF 공격 방어: 다른 사이트에서의 요청 시 쿠키 전송 제한
        return cookie;
    }

    public Cookie expire() {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, null);
        cookie.setMaxAge(0);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}
