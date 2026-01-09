package com.min.meow.user.filter;

import com.min.meow.global.Token;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.repository.RefreshTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class CustomLogoutFilter extends GenericFilter {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException,ServletException{

        String requestUri = request.getRequestURI();
        if (!requestUri.equals("/api/logout")) {
            filterChain.doFilter(request, response);
            return;
        }
        String requestMethod = request.getMethod();
        if (!requestMethod.equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 쿠키에서 refresh 토큰 추출
        String refresh = null;
        Cookie[] cookies = request.getCookies();

        // 쿠키가 없는 경우 처리
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refresh = cookie.getValue();
                }
            }
        }

        // refresh 토큰이 있는 경우에만 처리
        // 토큰이 없거나 만료되었어도 로그아웃은 성공
        if (refresh != null) {
            try {
                // 토큰이 유효한 경우에만 DB에서 제거 시도
                // 만료되었거나 유효하지 않은 토큰은 무시
                Token category = jwtUtil.getTokenCategory(refresh);
                if (category.equals(Token.REFRESH_TOKEN)) {
                    // DB에 저장되어 있으면 제거
                    if (refreshTokenRepository.existsByRefreshToken(refresh)) {
                        refreshTokenRepository.deleteByRefreshToken(refresh);
                    }
                }
            } catch (ExpiredJwtException e) {
                // 토큰이 만료된 경우에도 로그아웃 처리를 계속 진행
                // DB에서 만료된 토큰 제거 시도 (있다면)
                try {
                    refreshTokenRepository.deleteByRefreshToken(refresh);
                } catch (Exception ex) {
                    // DB 삭제 실패해도 쿠키는 제거
                }
            } catch (Exception e) {
                // 기타 예외 발생 시에도 로그아웃 처리 계속 진행
            }
        }

        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"로그아웃 성공\"}");
        return;
    }
}
