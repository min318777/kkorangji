package com.min.meow.user.jwt;


import com.min.meow.user.domain.CustomUserDetails;
import com.min.meow.user.entity.User;
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


@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {


    private final JwtUtils jwtUtils;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // request에서 Authorization 헤더를 찾음
        String authorization = request.getHeader("Authorization");

        if(authorization == null || !authorization.startsWith("Bearer ")){

            System.out.println("토큰이 없습니다.");
            filterChain.doFilter(request, response);
            // 조건이 해당되면 메소드 종료
            return;
        }

        String token = authorization.split(" ")[1];

        // 토큰 소멸시간 검증
        if (jwtUtils.isExpiration(token)){

            System.out.println("토큰이 만료되었습니다.");
            filterChain.doFilter(request, response);

            return;
        }

        // 토큰에서 loginId와 role 추출
        String loginId = jwtUtils.getLoginId(token);
        String role = jwtUtils.getRole(token);

        User user = User.builder()
                .loginId(loginId)
                .password("tempPassword")
                .build();
        // Userdetails에 회원 정보 객체 담기
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        // 스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        // 세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
