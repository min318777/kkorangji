package com.min.meow.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token Redis 저장 서비스
 * 키: "refresh:{userId}" -> refreshToken 값 저장
 * TTL: 14일 후 자동 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    // Refresh Token 만료 시간: 14일
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 14;

    private static final String KEY_PREFIX = "refresh:";

    /**
     * Refresh Token 저장
     */
    public void save(Long userId, String refreshToken) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TOKEN_EXPIRATION_DAYS, TimeUnit.DAYS);
        log.info("Refresh Token 저장 - userId: {}", userId);
    }

    /**
     * Refresh Token 조회
     */
    public String findByUserId(Long userId) {
        String key = KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 저장된 토큰과 일치하는지 확인
     */
    public boolean validateToken(Long userId, String refreshToken) {
        String savedToken = findByUserId(userId);
        return refreshToken.equals(savedToken);
    }

    /**
     * Refresh Token 삭제 (로그아웃 시 사용)
     */
    public void delete(Long userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("Refresh Token 삭제 - userId: {}", userId);
    }
}
