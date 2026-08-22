package com.min.meow.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정을 중앙에서 관리하는 Record.
 * application.yml의 jwt.* 프로퍼티와 바인딩됨.
 * @param secret           JWT 서명에 사용할 비밀 키
 * @param accessTtlMinutes Access Token 만료 시간 (30분)
 * @param refreshTtlDays   Refresh Token 만료 시간 (14일)
 * @param issuer           토큰 발행자 (iss 클레임)
 * @param audience         토큰 대상자 (aud 클레임)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
        String secret,
        int accessTtlMinutes,
        int refreshTtlDays,
        String issuer,
        String audience
) {
    public long accessTtlMillis() {
        return (long) accessTtlMinutes * 60 * 1000;
    }

    public long refreshTtlMillis() {
        return (long) refreshTtlDays * 24 * 60 * 60 * 1000;
    }
}
