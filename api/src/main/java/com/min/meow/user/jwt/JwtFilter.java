package com.min.meow.user.jwt;


import com.min.meow.global.Role;
import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.domain.CustomUserDetails;
import com.min.meow.user.entity.User;
import com.nimbusds.oauth2.sdk.auth.JWTAuthentication;
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


@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        /*
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
        Role role = jwtUtils.getRole(token);

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
    */

        // 헤더에서 access키에 담긴 토큰을 꺼냄
        //String accessToken = request.getHeader("access");
        String path = request.getRequestURI();

        // 리프레시 토큰 요청은 토큰 검사하지 않고 바로 필터 체인으로 넘기기
        if ("/reissue".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }else if(path.equals("/user/login")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.equals("/user/join")
                || path.equals("/meow/lost-cat")
                || path.equals("/meow/lost-cat/comment/1")
                || path.equals("/notice")
        ){
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");

        if(authorization == null || !authorization.startsWith("Bearer ")){

            System.out.println("토큰이 없습니다.");
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

        // 토큰에서 loginId와 role 추출
        String loginId = jwtUtil.getLoginId(accessToken);
        Role role = jwtUtil.getRole(accessToken);

        User user = User.builder()
                .loginId(loginId)
                .role(role)
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
