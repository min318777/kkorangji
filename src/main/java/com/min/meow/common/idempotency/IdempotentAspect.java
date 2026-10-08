package com.min.meow.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.common.ApiResponse;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final String KEY_PREFIX = "idem:";
    private static final String PROCESSING_MARKER = "PROCESSING";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Around("@annotation(Idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        String idempotencyKey = extractIdempotencyKey();
        String redisKey = KEY_PREFIX + idempotencyKey;

        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, PROCESSING_MARKER, TTL);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            return handleDuplicateRequest(redisKey);
        }

        try {
            Object result = joinPoint.proceed();
            if (result instanceof ResponseEntity<?> responseEntity) {
                redisTemplate.opsForValue().set(redisKey, serialize(responseEntity.getBody()), TTL);
            }
            return result;
        } catch (Throwable e) {
            // 처리 실패 시 선점을 풀어줘야 재시도가 막히지 않음
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    // 캐싱된 응답은 body(ApiResponse)만 복원하고, 상태코드는 항상 201(Created)로 고정
    // (이 Aspect가 붙는 대상은 전부 리소스 생성 API라 재시도 응답도 201이 맞음)
    private Object handleDuplicateRequest(String redisKey) {
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached == null || PROCESSING_MARKER.equals(cached)) {
            throw new CustomException(ErrorCode.DUPLICATE_REQUEST_IN_PROGRESS);
        }
        try {
            ApiResponse<?> body = objectMapper.readValue(cached, ApiResponse.class);
            return ResponseEntity.status(201).body(body);
        } catch (Exception e) {
            log.error("[Idempotency] 캐싱된 응답 역직렬화 실패 - key: {}", redisKey, e);
            throw new CustomException(ErrorCode.DUPLICATE_REQUEST_IN_PROGRESS);
        }
    }

    private String extractIdempotencyKey() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new CustomException(ErrorCode.MISSING_IDEMPOTENCY_KEY);
        }
        HttpServletRequest request = attributes.getRequest();
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            throw new CustomException(ErrorCode.MISSING_IDEMPOTENCY_KEY);
        }
        return key;
    }

    private String serialize(Object result) throws Exception {
        return objectMapper.writeValueAsString(result);
    }
}
