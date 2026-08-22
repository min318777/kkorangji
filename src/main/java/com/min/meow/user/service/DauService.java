package com.min.meow.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

/**
 * DAU(Daily Active Users) 측정 서비스
 * Redis SET 자료구조로 하루 단위 고유 로그인 유저 수를 관리합니다.
 * SET은 중복을 자동 제거하므로 같은 유저가 여러 번 로그인해도 1명으로 카운트됩니다.
 * 키 구조:
 *   dau:{날짜} → SET { "1", "5", "42", ... }  TTL 7일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DauService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String DAU_KEY_PREFIX = "dau:";

    /** 로그인 시 호출 — 오늘 DAU SET에 userId 추가 */
    public void record(Long userId) {
        try {
            String key = DAU_KEY_PREFIX + LocalDate.now();
            redisTemplate.opsForSet().add(key, String.valueOf(userId));
            redisTemplate.expire(key, Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("DAU 기록 실패 - userId: {}", userId, e);
        }
    }

    /** 모든 요청 시 호출 — IP 기반 DAU SET에 추가 (비로그인 포함) */
    public void recordByIp(String ip) {
        try {
            String key = DAU_KEY_PREFIX + "ip:" + LocalDate.now();
            redisTemplate.opsForSet().add(key, ip);
            redisTemplate.expire(key, Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("DAU IP 기록 실패 - ip: {}", ip, e);
        }
    }

    /** 특정 날짜의 로그인 유저 DAU 조회 */
    public Long getCount(LocalDate date) {
        try {
            Long count = redisTemplate.opsForSet().size(DAU_KEY_PREFIX + date);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("DAU 조회 실패 - date: {}", date, e);
            return 0L;
        }
    }

    /** 특정 날짜의 IP 기반 DAU 조회 (비로그인 포함 전체 방문자) */
    public Long getIpCount(LocalDate date) {
        try {
            Long count = redisTemplate.opsForSet().size(DAU_KEY_PREFIX + "ip:" + date);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("DAU IP 조회 실패 - date: {}", date, e);
            return 0L;
        }
    }
}
