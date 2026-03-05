package com.min.meow.security.jwt;


import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.security.dto.CustomUserDetails;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


/**
 * JWT 인증 필터
 * 역할: Authorization 헤더에서 JWT 토큰을 추출하고 검증하여 SecurityContext에 인증 정보를 설정
 * 동작 방식:
 * 1. 토큰이 있으면 → 검증 후 인증 설정, 다음 필터로 진행
 * 2. 토큰이 없으면 → 인증 없이 다음 필터로 진행 (SecurityConfig의 AuthorizationFilter가 접근 제어)
 * 3. 토큰이 유효하지 않으면 → 에러 응답 반환 (필터 체인 중단)
 * 주의: 접근 제어(permitAll, authenticated)는 SecurityConfig에서만 관리
 */
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Authorization 헤더에서 토큰 추출
        String authorization = request.getHeader("Authorization");

        // 토큰이 없으면 인증 없이 다음 필터로 진행
        // SecurityConfig의 AuthorizationFilter가 접근 권한을 체크함
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorization.substring(7); // "Bearer " 제거

        // decodeAndVerify로 1회 파싱: 서명 + 만료 + issuer + audience + 타입(ACCESS) 검증
        Claims claims;
        try {
            claims = jwtUtil.decodeAndVerify(accessToken, Token.ACCESS_TOKEN);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰으로 접근 시도: {}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다.");
            return;
        } catch (CustomException e) {
            // 토큰 타입 불일치 (Refresh Token으로 API 접근 시도 등)
            log.warn("Access 토큰이 아닌 토큰으로 접근 시도");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "유효하지 않은 Access 토큰입니다.");
            return;
        } catch (JwtException e) {
            log.warn("유효하지 않은 토큰: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
            return;
        }

        // Claims에서 userId 추출 및 사용자 조회
        Long userId = Long.valueOf(claims.getSubject());
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            log.warn("토큰의 사용자를 찾을 수 없음 - userId: {}", userId);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
            return;
        }

        User user = userOptional.get();

        // UserDetails에 회원 정보 객체 담기
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        // 스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                customUserDetails, null, customUserDetails.getAuthorities());

        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 에러 응답을 직접 작성하여 반환
     * 필터에서는 예외를 throw하면 안 되므로 response에 직접 에러를 작성
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String jsonResponse = String.format(
                "{\"error\": \"%s\", \"message\": \"%s\", \"status\": %d}",
                errorCode, message, status
        );

        response.getWriter().write(jsonResponse);
    }
}
