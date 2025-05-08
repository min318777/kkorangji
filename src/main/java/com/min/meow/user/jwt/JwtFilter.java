package com.min.meow.user.jwt;


import com.min.meow.global.Role;
import com.min.meow.global.Token;
import com.min.meow.user.domain.CustomUserDetails;
import com.min.meow.user.entity.User;
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
import java.io.PrintWriter;


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



        /*
        // 헤더에서 access키에 담긴 토큰을 꺼냄
        String accessToken = request.getHeader("access");

        // 토큰이 없다면 다음 필터로 넘김
        if (accessToken == null) {

            filterChain.doFilter(request, response);

            return;
        }

        // 토큰 만료 여부 확인, 만료시 다음 필터로 넘기지 않음
        try {
            jwtUtils.isExpiration(accessToken);
        } catch (ExpiredJwtException e) {

            //response body
            PrintWriter writer = response.getWriter();
            writer.print("토큰이 만료되었습니다.");

            //response status code
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 토큰이 access인지 확인 (발급시 페이로드에 명시)
        Token category = jwtUtils.getTokenCategory(accessToken);

        if (!category.equals(Token.ACCESS_TOKEN)) {

            //response body
            PrintWriter writer = response.getWriter();
            writer.print("유효하지 않은 access 토큰 입니다.");

            //response status code
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }


        // username, role 값을 획득
        String loginId = jwtUtils.getLoginId(accessToken);
        Role role = jwtUtils.getRole(accessToken);

        User user = new User();
        user.setLoginId(loginId);
        user.setRole(role);
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
    */


}
