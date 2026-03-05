package com.min.meow.security.jwt;

import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 유틸리티.
 *
 * <p>주요 개선점:
 * <ul>
 *   <li>{@link JwtConfig}로 TTL·issuer·audience를 중앙 관리 (하드코딩 제거)</li>
 *   <li>{@link #decodeAndVerify}로 1회 파싱 + 서명·만료·타입·issuer·audience 검증</li>
 *   <li>Access Token에 role 유지 (DB 조회 없이 권한 확인 가능)</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final JwtConfig config;

    public JwtUtil(JwtConfig config) {
        this.config = config;
        this.secretKey = Keys.hmacShaKeyFor(config.secret().getBytes(StandardCharsets.UTF_8));
    }

    // ============ 토큰 생성 ============

    /**
     * Access Token 생성 (subject: userId, payload: role, token 타입)
     * TTL은 JwtConfig에서 중앙 관리 — 호출자가 지정할 필요 없음.
     */
    public String createAccessToken(Long userId, String role) {
        return Jwts.builder()
                .claims()
                .subject(String.valueOf(userId))
                .add("token", Token.ACCESS_TOKEN.name())
                .add("role", role)
                .and()
                .issuer(config.issuer())
                .audience().add(config.audience()).and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + config.accessTtlMillis()))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 생성 (subject: userId, token 타입)
     * TTL은 JwtConfig에서 중앙 관리.
     */
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .claims()
                .subject(String.valueOf(userId))
                .add("token", Token.REFRESH_TOKEN.name())
                .and()
                .issuer(config.issuer())
                .audience().add(config.audience()).and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + config.refreshTtlMillis()))
                .signWith(secretKey)
                .compact();
    }

    // ============ 토큰 검증 ============

    /**
     * JWT 토큰을 1회 파싱하여 서명·만료·issuer·audience·타입을 모두 검증하고 Claims를 반환.
     *
     * <p>{@code parseSignedClaims()}가 내부적으로 수행하는 검증:
     * <ul>
     *   <li>서명 위조 → {@link io.jsonwebtoken.security.SignatureException}</li>
     *   <li>토큰 만료 → {@link io.jsonwebtoken.ExpiredJwtException}</li>
     *   <li>형식 오류 → {@link io.jsonwebtoken.MalformedJwtException}</li>
     *   <li>issuer 불일치 → {@link io.jsonwebtoken.IncorrectClaimException}</li>
     *   <li>audience 불일치 → {@link io.jsonwebtoken.IncorrectClaimException}</li>
     * </ul>
     *
     * <p>추가로 토큰 타입(ACCESS/REFRESH)을 검증하여 타입 불일치 시
     * {@link CustomException}({@link ErrorCode#INVALID_TOKEN_TYPE})을 던짐.
     *
     * @param token        원본 JWT 문자열
     * @param expectedType 기대하는 토큰 타입 (Token.ACCESS_TOKEN 또는 Token.REFRESH_TOKEN)
     * @return Claims      검증 완료된 payload (subject, role 등을 꺼내 쓸 수 있음)
     */
    public Claims decodeAndVerify(String token, Token expectedType) {
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

    // ============ 설정 접근 ============

    /**
     * JwtConfig를 외부에서 참조할 수 있도록 제공.
     * RefreshTokenService 등에서 TTL 값이 필요할 때 사용.
     */
    public JwtConfig getConfig() {
        return config;
    }
}
