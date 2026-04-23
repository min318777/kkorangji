package com.min.meow.security.jwt;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.global.ApiResponse;
import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.security.dto.CustomUserDetails;
import com.min.meow.security.service.PermissionCacheService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import org.slf4j.MDC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final PermissionCacheService permissionCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        // 토큰이 없으면 인증 없이 다음 필터로 진행
        // SecurityConfig의 AuthorizationFilter가 접근 권한을 체크함
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorization.substring(7);
        Claims claims;
        try {
            claims = jwtProvider.decodeAndVerify(accessToken, Token.ACCESS_TOKEN);
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

        // v1(DB 조회) vs v2(토큰 추출) vs v3(Redis 캐시) 분기
        // X-Auth-Version 헤더로 방식 선택
        String authVersion = request.getHeader("X-Auth-Version");
        CustomUserDetails principal;

        if ("v3".equals(authVersion)) {
            // v3: Redis에서 permissions 조회 — 캐시 히트 시 DB 조회 없음
            // 캐시 미스 or Redis 장애 시 DB fallback 후 재캐싱
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);

            List<String> permissions = permissionCacheService.getPermissions(userId);

            if (permissions == null) {
                // 캐시 미스 or Redis 장애 → DB에서 최신 권한 조회
                log.debug("v3 권한 캐시 미스 → DB fallback - userId: {}", userId);
                Optional<User> userOptional = userRepository.findById(userId);

                if (userOptional.isEmpty()) {
                    log.warn("토큰의 사용자를 찾을 수 없음 - userId: {}", userId);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
                    return;
                }

                User user = userOptional.get();

                permissions = List.copyOf(user.getAllPermissionCodes());

                // DB에서 조회한 최신 권한을 Redis에 재캐싱
                permissionCacheService.cachePermissions(userId, permissions);
            }

            principal = new CustomUserDetails(userId, role, permissions);

        } else if ("v2".equals(authVersion)) {
            // v2: Claims에서 직접 추출 — DB 조회 없음 (성능 최적화)
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);

            // permissions 추출 (RBAC 권한 목록)
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);

            principal = new CustomUserDetails(userId, role, permissions);
        } else {
            // v1: DB에서 사용자 조회 후 상태 검증
            Long userId = Long.valueOf(claims.getSubject());
            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isEmpty()) {
                log.warn("토큰의 사용자를 찾을 수 없음 - userId: {}", userId);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
                return;
            }

            User user = userOptional.get();

            // User 엔티티에서 최신 role/permissions 추출
            principal = CustomUserDetails.from(user);
        }

        // SecurityContext에 인증 정보 설정
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // MDC에 로그인 사용자 ID 추가 (MdcFilter의 requestId와 함께 사용)
        MDC.put("userId", String.valueOf(principal.getUserId()));

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 에러 응답을 직접 작성하여 반환
     * - 필터에서는 예외를 throw하면 안 되므로 response에 직접 에러를 작성
     * - ApiResponse.fail()을 사용하여 전체 API와 동일한 {status, success, message, data} 포맷 반환
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 전체 API와 동일한 포맷으로 응답 (errorCode는 로깅용이므로 메시지에만 반영)
        ApiResponse<Void> apiResponse = ApiResponse.fail(HttpStatus.valueOf(status), message);
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);

        response.getWriter().write(jsonResponse);
    }
}
