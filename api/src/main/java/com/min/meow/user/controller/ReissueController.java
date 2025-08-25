package com.min.meow.user.controller;


import com.min.meow.config.PrincipalUser;
import com.min.meow.global.Role;
import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.entity.RefreshToken;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.repository.RefreshTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class ReissueController {

    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;


    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @PostMapping("/api/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response, @AuthenticationPrincipal PrincipalUser user){

        String refresh = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refresh")) {
                refresh = cookie.getValue();
            }
        }

        if (refresh == null) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        // 토큰이 refresh인지 확인 (발급시 페이로드에 명시)
        Token category = jwtUtil.getTokenCategory(refresh);
        if (!category.equals(Token.REFRESH_TOKEN)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        
        }

        // DB에 저장되어 있는지 확인
        boolean isExist = refreshTokenRepository.existsByRefreshToken(refresh);
        if(!isExist){
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        String loginId = jwtUtil.getLoginId(refresh);
        Long userId = Long.valueOf(jwtUtil.getUserId(refresh));
        String role = jwtUtil.getRole(refresh);
        
        // 새로운 jwt발급
        String newAccess = jwtUtil.createAccessToken(userId, Token.ACCESS_TOKEN, loginId, role, 600000L);
        String newRefresh = jwtUtil.createRefreshToken(userId, REFRESH_TOKEN_EXPIRATION, Token.REFRESH_TOKEN);
        
        // refresh 토큰 저장 db에 기존의 refresh토큰 삭제후 새 refresh토큰 저장
        RefreshToken existingEntity = refreshTokenRepository.findByRefreshToken(refresh);
        if(existingEntity != null) {
            // 기존 엔티티 업데이트
            existingEntity.setRefreshToken(newRefresh);
            refreshTokenRepository.save(existingEntity);
        } else {
            // 새로 생성
            addRefreshToken(loginId, userId, newRefresh);
        }

        // 응답
        response.setHeader("Authorization", newAccess);
        response.addCookie(createCookie("refresh", newRefresh));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        //cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return  cookie;
    }

    //Refresh 토큰 저장소에서 기한이 지난 토큰 삭제
    //TTL 설정을 통해 자동으로 Refresh 토큰이 삭제되면 무방하지만 계속해서 토큰이 쌓일 경우 용량 문제가 발생할 수 있다.
    //
    //따라서 스케줄 작업을 통해 만료시간이 지난 토큰은 주기적으로 삭제하는 것이 올바르다.
    // -> 레디스로 구현하면 편할듯?
    //
    private void addRefreshToken(String loginId, Long userId, String refresh) {

        RefreshToken refreshToken = RefreshToken.builder()
                .loginId(loginId)
                .refreshToken(refresh)
                .userId(userId)
                .expiration(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_EXPIRATION / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}


