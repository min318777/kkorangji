package com.min.meow.user.service;

import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.dto.response.TokenResponse;
import com.min.meow.user.entity.User;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReissueService {

    // Access Token: 30분
    private static final long ACCESS_TOKEN_EXPIRATION = 30 * 60 * 1000L;
    // Refresh Token: 14일
    private static final long REFRESH_TOKEN_EXPIRATION = 14 * 24 * 60 * 60 * 1000L;

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    /**
     * 토큰 재발급
     * 1. Refresh Token 유효성 검증
     * 2. Redis에 저장된 토큰과 일치하는지 확인
     * 3. 새로운 Access Token, Refresh Token 발급 (토큰 로테이션)
     * 4. Redis에 새 Refresh Token 저장
     */
    public TokenResponse reissue(String refreshToken) {
        // 1. 토큰 null 체크
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        // 2. 토큰 만료 확인
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        // 3. Refresh Token인지 확인
        Token category = jwtUtil.getTokenCategory(refreshToken);
        if (!category.equals(Token.REFRESH_TOKEN)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 4. Redis에 저장된 토큰과 일치하는지 확인
        Long userId = jwtUtil.getUserId(refreshToken);
        if (!refreshTokenService.validateToken(userId, refreshToken)) {
            // Reuse Detection: 이미 사용된(또는 유효하지 않은) 토큰으로 재발급 시도 시
            // 보안을 위해 현재 저장된 유효한 토큰도 모두 삭제하여 강제 로그아웃 처리
            log.warn("이미 사용된 리프레쉬토큰으로 재발급 요청: {}", userId);
            refreshTokenService.delete(userId);
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 5. DB에서 최신 사용자 정보 조회 (role 변경 반영)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
        String role = user.getRole().name();

        // 6. 새로운 토큰 발급 (토큰 로테이션)
        String newAccessToken = jwtUtil.createAccessToken(userId, Token.ACCESS_TOKEN, role, ACCESS_TOKEN_EXPIRATION);
        String newRefreshToken = jwtUtil.createRefreshToken(userId, Token.REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION);

        // 7. Redis에 새 Refresh Token 저장 (기존 토큰 덮어쓰기)
        refreshTokenService.save(userId, newRefreshToken);
        log.info("토큰 재발급 완료 - userId: {}", userId);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}
