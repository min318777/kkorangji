package com.min.meow.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.security.userdetails.CustomUserDetails;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.security.jwt.JwtProvider;
import com.min.meow.security.service.PermissionCacheService;
import com.min.meow.security.service.RefreshTokenService;
import com.min.meow.user.service.DauService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final PermissionCacheService permissionCacheService;
    private final DauService dauService;
    private final ObjectMapper objectMapper;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
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

        // authorities에서 Role과 Permission을 분리
        // ROLE_ 접두사가 있으면 Role, 없으면 Permission
        Set<String> allAuthorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // 첫 번째 Role을 대표 role로 사용 (JWT의 role 필드)
        String role = allAuthorities.stream()
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_USER");

        // Permission 목록 추출
        List<String> permissions = allAuthorities.stream()
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toList());

        // 토큰 생성 — TTL은 JwtConfig에서 중앙 관리, permissions 포함
        String accessToken = jwtProvider.createAccessToken(userId, role, permissions);
        JwtProvider.RefreshTokenInfo refreshInfo = jwtProvider.createRefreshToken(userId);

        // jti(JWT ID)만 Redis에 저장 (전체 JWT 대신 짧은 UUID로 메모리 절약)
        refreshTokenService.save(userId, refreshInfo.jti());

        // v3 권한 캐싱 — 로그인 시 점에서 permissions를 Redis에 저장
        // 이후 요청에서 DB 조회 없이 Redis에서 권한 확인 가능
        permissionCacheService.cachePermissions(userId, permissions);
        dauService.record(userId);

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
        // 탈퇴한 사용자와 일반 인증 실패를 구분하여 메시지 반환
        String message = (failed.getCause() instanceof DisabledException || failed instanceof DisabledException)
                ? "탈퇴한 사용자입니다."
                : "아이디 또는 비밀번호가 일치하지 않습니다.";

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }

    private Cookie createRefreshCookie(String value) {
        int refreshTtlSeconds = jwtProvider.getConfig().refreshTtlDays() * 24 * 60 * 60;
        Cookie cookie = new Cookie("refresh", value);
        cookie.setMaxAge(refreshTtlSeconds);
        //cookie.setSecure(true);
        cookie.setPath("/"); // 쿠키가 적용될 범위
        cookie.setHttpOnly(true); // XSS 공격 방어: 자바스크립트로 쿠키 접근 불가
        cookie.setAttribute("SameSite", "Lax"); // CSRF 공격 방어: 다른 사이트에서의 요청 시 쿠키 전송 제한
        return cookie;
    }
}
