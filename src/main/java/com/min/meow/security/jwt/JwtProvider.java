package com.min.meow.security.jwt;

import com.min.meow.common.TokenType;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final JwtConfig config;

    public JwtProvider(JwtConfig config) {
        this.config = config;
        this.secretKey = Keys.hmacShaKeyFor(config.secret().getBytes(StandardCharsets.UTF_8));
    }


    // Access Token 생성 (subject: userId, payload: role, permissions, token 타입)
    public String createAccessToken(Long userId, String role, List<String> permissions) {
        return Jwts.builder()
                .claims()
                .subject(String.valueOf(userId))
                .add("token", TokenType.ACCESS_TOKEN.name())
                .add("role", role)
                .add("permissions", permissions)
                .and()
                .issuer(config.issuer())
                .audience().add(config.audience()).and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + config.accessTtlMillis()))
                .signWith(secretKey)
                .compact();
    }

    // Refresh Token 생성 결과 — token: 클라이언트 전달용 JWT, jti: Redis 저장용 짧은 UUID
    public record RefreshTokenInfo(String token, String jti) {}

    // Refresh Token 생성 (subject: userId, token 타입, jti: UUID) — jti만 Redis에 저장해 용량 절약
    public RefreshTokenInfo createRefreshToken(Long userId) {
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .claims()
                .subject(String.valueOf(userId))
                .add("token", TokenType.REFRESH_TOKEN.name())
                .id(jti)
                .and()
                .issuer(config.issuer())
                .audience().add(config.audience()).and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + config.refreshTtlMillis()))
                .signWith(secretKey)
                .compact();
        return new RefreshTokenInfo(token, jti);
    }

    public Claims decodeAndVerify(String token, TokenType expectedType) {
        // 1회 파싱: 서명 + 만료 + issuer + audience 검증을 한 번에 수행
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(config.issuer())
                .requireAudience(config.audience())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 토큰 타입 검증 (ACCESS인데 REFRESH를 보낸 경우 등)
        String tokenType = claims.get("token", String.class);
        if (!expectedType.name().equals(tokenType)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN_TYPE);
        }
        return claims;
    }

    // JwtConfig 외부 참조용 — RefreshTokenService 등에서 TTL 값 필요할 때 사용
    public JwtConfig getConfig() {
        return config;
    }
}
