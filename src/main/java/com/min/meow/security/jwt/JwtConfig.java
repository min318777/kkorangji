package com.min.meow.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
