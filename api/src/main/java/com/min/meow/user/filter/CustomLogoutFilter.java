package com.min.meow.user.filter;

import com.min.meow.global.Token;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.service.RefreshTokenService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class CustomLogoutFilter extends OncePerRequestFilter {

    private static final String LOGOUT_URI = "/api/logout";
    private static final String REFRESH_COOKIE_NAME = "refresh";

    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!isLogoutRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = extractRefreshToken(request);

        if (refreshToken != null) {
            handleRefreshToken(refreshToken);
        }

        expireRefreshCookie(response);
        writeLogoutResponse(response);
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        return LOGOUT_URI.equals(request.getRequestURI())
                && "POST".equalsIgnoreCase(request.getMethod());
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void handleRefreshToken(String refreshToken) {
        try {
            Token category = jwtUtil.getTokenCategory(refreshToken);

            // Refresh Token인지 확인
            if (category == Token.REFRESH_TOKEN) {
                // 토큰에서 userId 추출하여 Redis에서 삭제
                Long userId = jwtUtil.getUserId(refreshToken);
                refreshTokenService.delete(userId);
                log.info("로그아웃 완료 - userId: {}", userId);
            }

        } catch (ExpiredJwtException e) {
            // 만료된 토큰이라도 userId 추출 시도
            log.info("만료된 Refresh Token 로그아웃 처리");
            try {
                Long userId = jwtUtil.getUserId(refreshToken);
                refreshTokenService.delete(userId);
            } catch (Exception ex) {
                log.warn("만료된 토큰에서 userId 추출 실패", ex);
            }

        } catch (Exception e) {
            // 로그아웃은 항상 성공해야 하므로 예외는 삼키되 로그는 남김
            log.warn("로그아웃 처리 중 예외 발생", e);
        }
    }

    private void expireRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void writeLogoutResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"로그아웃 성공\"}");
    }
}
