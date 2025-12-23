package com.min.meow.user.jwt;


import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.dto.CustomUserDetails;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/api/users/login",
            "/api/users/join",
            "/api/reissue",
            "/api/meow/boast-cat",
            "/api/meow/lost-cat",
            "/api/logout",
            "/api/notification",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 화이트리스트 경로는 JWT 검증을 건너뜀 (로그아웃 포함)
        if (isWhitelistPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if(authorization == null || !authorization.startsWith("Bearer ")){
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
            //filterChain.doFilter(request, response);
            // 조건이 해당되면 메소드 종료
        }
        String accessToken = authorization.split(" ")[1];

        // 토큰이 없다면 다음 필터로 넘김
        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 만료 여부 확인, 만료시 다음 필터로 넘기지 않음
        try {
            jwtUtil.isExpired(accessToken);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        // 토큰이 access인지 확인 (발급시 페이로드에 명시)
        Token category = jwtUtil.getTokenCategory(accessToken);
        if (!category.equals(Token.ACCESS_TOKEN)) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        // 토큰에서 loginId추출
        String loginId = jwtUtil.getLoginId(accessToken);
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // Userdetails에 회원 정보 객체 담기
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        // 스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        // 세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }

    private boolean isWhitelistPath(String path) {
        return WHITELIST_PATHS.stream()
                .anyMatch(whitelistPath -> {
                    // exact match 또는 prefix match 지원
                    return path.equals(whitelistPath) || path.startsWith(whitelistPath);
                });
    }
}
