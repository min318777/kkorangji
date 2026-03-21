package com.min.meow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정 클래스
 * Spring의 @Async 어노테이션을 사용한 비동기 메서드 실행을 위한 설정입니다.
 * ThreadPoolTaskExecutor를 사용하여 스레드 풀을 관리합니다.
 * 왜 ThreadPoolTaskExecutor를 사용하는가?
 * 1. 스레드 풀 관리: 스레드를 미리 생성해두고 재사용하여 오버헤드 감소
 * 2. 자원 제한: 동시에 실행되는 스레드 수를 제한하여 시스템 안정성 확보
 * 3. 큐잉: 요청이 많을 때 큐에 대기시켜 순차적 처리 가능
 * 4. 모니터링: 스레드 풀 상태 모니터링 가능
 * @EnableAsync: Spring의 비동기 처리 기능을 활성화합니다.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 알림 처리 전용 비동기 Executor
     * 설정값 설명:
     * - corePoolSize: 기본적으로 유지되는 스레드 수 (5개) -> 평상시 알림 처리에 필요한 최소 스레드
     * - maxPoolSize: 최대 스레드 수 (20개) -> 트래픽 급증 시 확장 가능한 최대 스레드
     * - queueCapacity: 대기 큐 크기 (100개) -> 모든 스레드가 바쁠 때 대기할 수 있는 작업 수
     * - keepAliveSeconds: 유휴 스레드 유지 시간 (60초) -> corePoolSize 초과 스레드가 유휴 상태일 때 종료까지 대기 시간
     * - threadNamePrefix: 스레드 이름 접두사 -> 로그에서 알림 처리 스레드를 쉽게 식별
     * - waitForTasksToCompleteOnShutdown: 종료 시 작업 완료 대기 -> 애플리케이션 종료 시 진행 중인 알림 처리 완료 보장
     * - awaitTerminationSeconds: 종료 대기 시간 (30초) -> 최대 30초까지 작업 완료를 기다림
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 코어 스레드 풀 크기: 항상 유지되는 스레드 수
        executor.setCorePoolSize(5);

        // 최대 스레드 풀 크기: 트래픽 급증 시 확장 가능한 최대치
        executor.setMaxPoolSize(20);

        // 작업 대기 큐 크기: 스레드가 모두 바쁠 때 대기 가능한 작업 수
        executor.setQueueCapacity(100);

        // 유휴 스레드 유지 시간 (초): 이 시간 이후 초과 스레드 종료
        executor.setKeepAliveSeconds(60);

        // 스레드 이름 접두사: 로그에서 식별 용이
        executor.setThreadNamePrefix("notification-async-");

        // 애플리케이션 종료 시 진행 중인 작업 완료까지 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 종료 시 최대 대기 시간 (초)
        executor.setAwaitTerminationSeconds(30);

        // 스레드 풀 초기화
        executor.initialize();

        log.debug("알림 비동기 처리 ThreadPoolTaskExecutor 초기화 완료 - core: {}, max: {}, queue: 100",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * 기본 비동기 Executor 설정
     * @Async 어노테이션에서 executor를 지정하지 않을 경우 사용됩니다.
     * 알림 외 다른 비동기 작업에도 사용할 수 있습니다.
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-default-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.debug("기본 비동기 ThreadPoolTaskExecutor 초기화 완료");

        return executor;
    }

    /**
     * 비동기 예외 처리기
     * @Async 메서드에서 발생한 예외를 처리합니다.
     * 비동기 메서드는 호출자에게 예외를 전파하지 않으므로,
     * 여기서 로깅하여 문제를 추적할 수 있습니다.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    /**
     * 비동기 예외 핸들러 내부 클래스
     * 비동기 메서드에서 발생한 예외를 로깅합니다.
     */
    private static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(AsyncExceptionHandler.class);

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("비동기 메서드 예외 발생 - {}.{}()",
                    method.getDeclaringClass().getSimpleName(), method.getName(), ex);
        }
    }
}