package com.min.meow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정 클래스
 * Redis 장애 시에도 서비스가 정상 동작하도록 DB Fallback 처리를 제공합니다.
 * CacheErrorHandler를 통해 캐시 연산 실패 시 예외를 무시하고 DB에서 직접 조회합니다.
 * 동작 방식:
 * 1. 캐시 조회 실패 (Redis 장애) → 예외 무시 → DB에서 조회
 * 2. 캐시 저장 실패 (Redis 장애) → 예외 무시 → 다음 요청 시 재시도
 * 3. 캐시 삭제 실패 (Redis 장애) → 예외 무시 → 로그 기록
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /**
     * 캐시 에러 핸들러
     * Redis 연결 실패, 타임아웃 등의 상황에서 예외를 무시하고
     * 애플리케이션이 계속 동작할 수 있도록 합니다.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            /**
             * 캐시 조회 실패 시 호출
             * Redis에서 데이터를 가져오지 못하면 DB에서 직접 조회합니다.
             */
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis 캐시 조회 실패 - cache: {}, key: {}, error: {}. DB에서 조회합니다.",
                        cache.getName(), key, exception.getMessage());
            }

            /**
             * 캐시 저장 실패 시 호출
             * DB 조회 결과를 Redis에 저장하지 못해도 응답은 정상 반환됩니다.
             */
            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Redis 캐시 저장 실패 - cache: {}, key: {}, error: {}. 다음 요청 시 재시도합니다.",
                        cache.getName(), key, exception.getMessage());
            }

            /**
             * 캐시 삭제 실패 시 호출
             * @CacheEvict 실패 시에도 비즈니스 로직은 정상 수행됩니다.
             */
            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis 캐시 삭제 실패 - cache: {}, key: {}, error: {}",
                        cache.getName(), key, exception.getMessage());
            }

            /**
             * 캐시 전체 삭제 실패 시 호출
             * @CacheEvict(allEntries = true) 실패 시에도 비즈니스 로직은 정상 수행됩니다.
             */
            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Redis 캐시 전체 삭제 실패 - cache: {}, error: {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
