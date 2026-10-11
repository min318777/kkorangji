package com.min.meow.security.service;

import com.min.meow.security.jwt.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

// Refresh Token Redis 저장 서비스 — 전체 JWT 대신 짧은 jti(UUID)만 저장해 메모리 절약
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String KEY_PREFIX = "auth:refresh:";

    // jti(JWT ID) 저장
    public void save(Long userId, String jti) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, jti, jwtConfig.refreshTtlDays(), TimeUnit.DAYS);
        log.debug("Refresh Token jti 저장 - userId: {}", userId);
    }

    // 저장된 jti 조회
    public String findByUserId(Long userId) {
        String key = KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    // 저장된 jti와 일치하는지 확인
    public boolean validateToken(Long userId, String jti) {
        String savedJti = findByUserId(userId);
        return jti.equals(savedJti);
    }

    // Refresh Token 삭제 (로그아웃 시 사용)
    public void delete(Long userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Refresh Token 삭제 - userId: {}", userId);
    }
}
