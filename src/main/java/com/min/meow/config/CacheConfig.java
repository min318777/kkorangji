package com.min.meow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

// 캐시 설정 — Redis 장애 시 CacheErrorHandler로 예외를 무시하고 DB Fallback 처리
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    // Redis 연결 실패/타임아웃 시 예외를 무시하고 애플리케이션이 계속 동작하도록 하는 캐시 에러 핸들러
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            // 캐시 조회 실패 시 호출 — DB에서 직접 조회하도록 넘김
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis 캐시 조회 실패 - cache: {}, key: {}, error: {}. DB에서 조회합니다.",
                        cache.getName(), key, exception.getMessage());
            }

            // 캐시 저장 실패 시 호출 — 저장 못 해도 응답은 정상 반환
            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Redis 캐시 저장 실패 - cache: {}, key: {}, error: {}. 다음 요청 시 재시도합니다.",
                        cache.getName(), key, exception.getMessage());
            }

            // 캐시 삭제 실패 시 호출 — @CacheEvict 실패해도 비즈니스 로직은 정상 수행
            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis 캐시 삭제 실패 - cache: {}, key: {}, error: {}",
                        cache.getName(), key, exception.getMessage());
            }

            // 캐시 전체 삭제 실패 시 호출 — @CacheEvict(allEntries=true) 실패해도 비즈니스 로직은 정상 수행
            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Redis 캐시 전체 삭제 실패 - cache: {}, error: {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
