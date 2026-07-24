package com.min.meow.security.service;

import com.min.meow.common.TokenType;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.security.dto.TokenResponse;
import com.min.meow.user.entity.User;
import com.min.meow.security.jwt.JwtProvider;
import com.min.meow.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReissueService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PermissionCacheService permissionCacheService;

    /**
     * 토큰 재발급
     * 1. Refresh Token 유효성 검증 (decodeAndVerify로 1회 파싱)
     * 2. Redis에 저장된 토큰과 일치하는지 확인
     * 3. 새로운 Access Token, Refresh Token 발급 (토큰 로테이션)
     * 4. Redis에 새 Refresh Token 저장
     */
    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        Claims claims;
        try {
            claims = jwtProvider.decodeAndVerify(refreshToken, TokenType.REFRESH_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        } catch (CustomException e) {
            // 토큰 타입 불일치 (Access Token으로 재발급 시도)
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. Claims에서 userId, jti 추출
        Long userId = Long.valueOf(claims.getSubject());
        String jti = claims.getId();

        // 4. Redis에 저장된 jti와 일치하는지 확인
        if (!refreshTokenService.validateToken(userId, jti)) {
            // Reuse Detection: 이미 사용된(또는 유효하지 않은) 토큰으로 재발급 시도 시
            // 보안을 위해 현재 저장된 유효한 토큰도 모두 삭제하여 강제 로그아웃 처리
            log.warn("이미 사용된 리프레쉬토큰으로 재발급 요청: {}", userId);
            refreshTokenService.delete(userId);
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 5. DB에서 최신 사용자 정보 조회 (role/permission 변경 반영)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 탈퇴한 유저는 재발급 불가 — Refresh Token도 삭제하여 강제 로그아웃
        if (user.isWithdrawn()) {
            refreshTokenService.delete(userId);
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        // 첫 번째 Role을 대표 role로 사용
        String role = user.getRoleNames().stream()
                .findFirst()
                .orElse("ROLE_USER");

        // Permission 목록 추출
        List<String> permissions = List.copyOf(user.getAllPermissionCodes());

        // 6. 새로운 토큰 발급 (토큰 로테이션) — TTL은 JwtConfig에서 중앙 관리
        String newAccessToken = jwtProvider.createAccessToken(userId, role, permissions);
        JwtProvider.RefreshTokenInfo newRefreshInfo = jwtProvider.createRefreshToken(userId);

        // 7. Redis에 새 jti 저장 (기존 jti 덮어쓰기)
        refreshTokenService.save(userId, newRefreshInfo.jti());

        // 8. v3 권한 캐시 재캐싱 — 재발급 시점에 DB에서 최신 권한을 이미 조회했으므로
        // 그대로 Redis에 업데이트 (권한 변경 후 토큰 재발급 시 자연스럽게 동기화)
        permissionCacheService.cachePermissions(userId, permissions);

        log.info("토큰 재발급 완료 - userId: {}, jti: {}", userId, newRefreshInfo.jti());

        return new TokenResponse(newAccessToken, newRefreshInfo.token());
    }
}
