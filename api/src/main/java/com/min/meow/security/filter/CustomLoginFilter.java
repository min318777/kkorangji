package com.min.meow.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.security.dto.CustomUserDetails;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.security.jwt.JwtUtil;
import com.min.meow.security.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);

            String loginId = loginRequest.getLoginId();
            String password = loginRequest.getPassword();

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(loginId, password);
            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        // 유저정보
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Long userId = userDetails.getUserId();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        // 토큰 생성 — TTL은 JwtConfig에서 중앙 관리
        String accessToken = jwtUtil.createAccessToken(userId, role, userDetails.getUsername());
        JwtUtil.RefreshTokenInfo refreshInfo = jwtUtil.createRefreshToken(userId);

        // jti(JWT ID)만 Redis에 저장 (전체 JWT 대신 짧은 UUID로 메모리 절약)
        refreshTokenService.save(userId, refreshInfo.jti());

        // 응답 설정 (쿠키에는 전체 JWT 토큰 사용)
        response.setHeader("Authorization", "Bearer " + accessToken);
        response.addCookie(createRefreshCookie(refreshInfo.token()));
        response.setStatus(HttpStatus.OK.value());

        // 사용자 정보 응답
        String responseBody = String.format(
                "{\"success\": true, \"accessToken\": \"%s\", \"userId\": %d, \"role\": \"%s\"}",
                accessToken, userId, role
        );
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseBody);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        // 보안: 아이디 미존재/비밀번호 불일치를 구분하지 않음 (계정 존재 여부 노출 방지)
        String message = "아이디 또는 비밀번호가 일치하지 않습니다.";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }

    private Cookie createRefreshCookie(String value) {
        int refreshTtlSeconds = jwtUtil.getConfig().refreshTtlDays() * 24 * 60 * 60;
        Cookie cookie = new Cookie("refresh", value);
        cookie.setMaxAge(refreshTtlSeconds);
        //cookie.setSecure(true);
        cookie.setPath("/"); // 쿠키가 적용될 범위
        cookie.setHttpOnly(true); // XSS 공격 방어: 자바스크립트로 쿠키 접근 불가
        cookie.setAttribute("SameSite", "Lax"); // CSRF 공격 방어: 다른 사이트에서의 요청 시 쿠키 전송 제한
        return cookie;
    }
}
