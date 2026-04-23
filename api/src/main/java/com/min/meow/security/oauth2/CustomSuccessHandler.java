package com.min.meow.security.oauth2;

import com.min.meow.security.jwt.JwtProvider;
import com.min.meow.security.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 발급받은 구글의 access토큰을 통해 구글 리소스서버에서 회원정보를 가져온 후 서비스의 jwt발급 및 프론트엔드에 전달 로직
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        Long userId = customUserDetails.getUserId();

        // Refresh Token 생성 — TTL은 JwtConfig에서 중앙 관리
        JwtProvider.RefreshTokenInfo refreshInfo = jwtProvider.createRefreshToken(userId);

        // jti(JWT ID)만 Redis에 저장 (전체 JWT 대신 짧은 UUID로 메모리 절약)
        refreshTokenService.save(userId, refreshInfo.jti());

        response.addCookie(createRefreshCookie(refreshInfo.token()));
        response.sendRedirect("http://localhost:3000/");
    }

    private Cookie createRefreshCookie(String value) {
        int refreshTtlSeconds = jwtProvider.getConfig().refreshTtlDays() * 24 * 60 * 60;
        Cookie cookie = new Cookie("refresh", value);
        cookie.setMaxAge(refreshTtlSeconds);
        //cookie.setSecure(true); -> https 통신시 필요
        cookie.setPath("/");
        cookie.setHttpOnly(true); // XSS 공격 방어
        return cookie;
    }
}
