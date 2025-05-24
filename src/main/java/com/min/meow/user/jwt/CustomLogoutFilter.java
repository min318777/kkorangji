package com.min.meow.user.jwt;

import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.repository.RefreshEntityRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class CustomLogoutFilter extends GenericFilter {
    private final RefreshEntityRepository refreshEntityRepository;
    private final JwtUtil jwtUtil;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException,ServletException{
        //path and method verify
        String requestUri = request.getRequestURI();
        if (!requestUri.matches("^\\/logout$")) {

            filterChain.doFilter(request, response);
            return;
        }
        String requestMethod = request.getMethod();
        if (!requestMethod.equals("POST")) {

            filterChain.doFilter(request, response);
            return;
        }

        //get refresh token
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {

            if (cookie.getName().equals("refresh")) {

                refresh = cookie.getValue();
            }
        }

        //refresh null check
        if (refresh == null) {

            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
            //response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        //expired check
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {

            //response status code
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
            //response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        // 토큰이 refresh인지 확인 (발급시 페이로드에 명시)
        Token category = jwtUtil.getTokenCategory(refresh);
        if (!category.equals(Token.REFRESH_TOKEN)) {

            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
            //response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        //DB에 저장되어 있는지 확인
        boolean isExist = refreshEntityRepository.existsByRefreshToken(refresh);
        if (!isExist) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);

            //response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        //로그아웃 진행
        //Refresh 토큰 DB에서 제거
        refreshEntityRepository.deleteByRefreshToken(refresh);

        //Refresh 토큰 Cookie 값 0
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");

        response.addCookie(cookie);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
