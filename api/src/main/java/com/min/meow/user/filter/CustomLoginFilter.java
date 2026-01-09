package com.min.meow.user.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.global.Token;
import com.min.meow.user.dto.CustomUserDetails;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.user.entity.RefreshToken;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.repository.RefreshTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;
    private static final long ACCESS_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000L;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

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

        String loginId = userDetails.getUsername();
        Long userId = userDetails.getUser().getId();
        String role = userDetails.getUser().getRole().name();

        // 토큰 생성
        String accessToken = jwtUtil.createAccessToken(userId, Token.ACCESS_TOKEN, loginId, role, ACCESS_TOKEN_EXPIRATION);
        String refreshToken = jwtUtil.createRefreshToken(userId, REFRESH_TOKEN_EXPIRATION, Token.REFRESH_TOKEN);

        // 리프레쉬토큰 저장
        addRefreshToken(loginId, userId, refreshToken);

        // 응답 설정
        response.setHeader("Authorization", "Bearer " + accessToken);
        response.addCookie(createCookie("refresh", refreshToken));
        response.setStatus(HttpStatus.OK.value());

        // 사용자 정보
        String responseBody = String.format(
                "{\"success\": true, \"accessToken\": \"%s\", \"loginId\": \"%s\", \"role\": \"%s\"}",
                accessToken, loginId, role
        );
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseBody);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        String message;

        if (failed instanceof UsernameNotFoundException) {
            message = "존재하지 않는 아이디입니다.";
        } else if (failed instanceof BadCredentialsException) {
            message = "비밀번호가 일치하지 않습니다.";
        } else {
            message = "로그인에 실패하였습니다.";
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }

    private Cookie createCookie(String key, String value){
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24*60*60);
        //cookie.setSecure(ture); -> https통신시 필요
        cookie.setPath("/"); //-> 쿠키가 적용될 범위
        cookie.setHttpOnly(true); // 자바스크립트로 해당쿠키에 접근하지 못하도록 하는 로직

        return cookie;
    }

    private void addRefreshToken(String loginId, Long userId, String refresh) {
        refreshTokenRepository.deleteByLoginId(loginId);
        RefreshToken refreshToken = RefreshToken.builder()
                .loginId(loginId)
                .refreshToken(refresh)
                .userId(userId)
                .expiration(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_EXPIRATION / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}
