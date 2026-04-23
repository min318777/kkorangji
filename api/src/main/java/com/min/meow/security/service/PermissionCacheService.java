package com.min.meow.security.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.security.jwt.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 권한(Permission) Redis 캐시 서비스 — v3 패턴
 *
 * <p>키: "permissions:{userId}" → JSON 직렬화된 권한 코드 목록
 * <p>TTL: Access Token 만료 시간과 동일하게 설정
 *
 * <p>동작 원리:
 * <ul>
 *   <li>로그인 시 권한 캐싱</li>
 *   <li>API 요청 시 Redis에서 조회 (DB 조회 없음)</li>
 *   <li>캐시 미스 or Redis 장애 시 null 반환 → 호출자가 DB fallback 처리</li>
 *   <li>권한 변경 / 탈퇴 시 캐시 무효화 → 다음 요청에서 DB 재조회</li>
 *   <li>토큰 재발급 시 최신 권한으로 재캐싱</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtConfig jwtConfig;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "permissions:";

    /**
     * 권한 목록을 Redis에 저장
     * TTL은 Access Token 만료 시간과 동일 (분 단위)
     */
    public void cachePermissions(Long userId, List<String> permissions) {
        try {
            String key = KEY_PREFIX + userId;
            String json = objectMapper.writeValueAsString(permissions);
            redisTemplate.opsForValue().set(key, json, jwtConfig.accessTtlMinutes(), TimeUnit.MINUTES);
            log.debug("권한 캐싱 완료 - userId: {}, permissions: {}", userId, permissions);
        } catch (Exception e) {
            // Redis 장애 시 캐싱 실패해도 로그인/요청은 정상 진행
            log.warn("권한 캐싱 실패 (Redis 장애 가능) - userId: {}, error: {}", userId, e.getMessage());
        }
    }

    /**
     * Redis에서 권한 목록 조회
     *
     * @return 권한 목록. 캐시 미스이거나 Redis 장애 시 null 반환 → 호출자가 DB fallback 처리
     */
    public List<String> getPermissions(Long userId) {
        try {
            String key = KEY_PREFIX + userId;
            String json = redisTemplate.opsForValue().get(key);

            if (json == null) {
                log.debug("권한 캐시 미스 - userId: {}", userId);
                return null;
            }

            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            // JSON 역직렬화 실패 — 캐시 데이터 손상 가능성, 재조회를 위해 null 반환
            log.warn("권한 캐시 역직렬화 실패 - userId: {}, error: {}", userId, e.getMessage());
            evictPermissions(userId); // 손상된 캐시 제거
            return null;
        } catch (Exception e) {
            // Redis 장애 시 null 반환 → 호출자가 DB fallback 처리
            log.warn("권한 캐시 조회 실패 (Redis 장애 가능) - userId: {}, error: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 권한 캐시 무효화
     * 권한 변경, 탈퇴 처리 시 호출 → 다음 요청에서 DB 재조회 후 재캐싱
     */
    public void evictPermissions(Long userId) {
        try {
            String key = KEY_PREFIX + userId;
            redisTemplate.delete(key);
            log.debug("권한 캐시 무효화 - userId: {}", userId);
        } catch (Exception e) {
            log.warn("권한 캐시 무효화 실패 (Redis 장애 가능) - userId: {}, error: {}", userId, e.getMessage());
        }
    }
}
