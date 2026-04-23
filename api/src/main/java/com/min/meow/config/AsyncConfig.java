package com.min.meow.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Map;
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

        // MDC TaskDecorator 등록: 부모 스레드의 MDC 컨텍스트를 자식 스레드에 전파
        // @Async로 실행되는 비동기 메서드에서도 requestId가 로그에 출력되도록 보장
        executor.setTaskDecorator(new MdcTaskDecorator());

        // 스레드 풀 초기화
        executor.initialize();

        log.debug("알림 비동기 처리 ThreadPoolTaskExecutor 초기화 완료 - core: {}, max: {}, queue: 100",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * 좋아요 DB 기록 전용 비동기 Executor
     * Redis 토글 후 DB 비동기 기록에 사용됩니다.
     * - CallerRunsPolicy: 큐 포화 시 호출 스레드에서 직접 실행 (요청 손실 방지)
     */
    @Bean(name = "likeExecutor")
    public Executor likeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("like-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
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
     * MDC 컨텍스트를 비동기 스레드로 전파하는 TaskDecorator
     *
     * 왜 필요한가?
     * - @Async 메서드는 새 스레드(ThreadPool)에서 실행됨
     * - 새 스레드는 부모 스레드의 MDC 컨텍스트(requestId 등)를 자동으로 상속하지 않음
     * - TaskDecorator로 부모 MDC를 복사하면 비동기 로그에도 동일한 requestId 출력 가능
     *
     * 동작 흐름:
     * HTTP 요청 스레드(MDC: requestId=abc) → @Async 실행
     * → TaskDecorator: 부모 MDC 복사 → 자식 스레드에 setContextMap
     * → NotificationEventListener 로그에 [abc] 출력
     * → 작업 완료 후 MDC.clear()
     */
    private static class MdcTaskDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {
            // 부모 스레드(HTTP 요청 스레드)의 MDC 컨텍스트 맵을 캡처
            // MDC.getCopyOfContextMap()은 null을 반환할 수 있으므로 null 체크 필요
            Map<String, String> parentMdcContext = MDC.getCopyOfContextMap();

            return () -> {
                try {
                    // 자식 스레드(비동기 스레드)에 부모 MDC 컨텍스트 적용
                    if (parentMdcContext != null) {
                        MDC.setContextMap(parentMdcContext);
                    }
                    // 실제 비동기 작업 실행 (e.g., 알림 저장, SSE 전송)
                    runnable.run();
                } finally {
                    // 스레드 풀에서 스레드가 재사용되므로 반드시 MDC 초기화
                    MDC.clear();
                }
            };
        }
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