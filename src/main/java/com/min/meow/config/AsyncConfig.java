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
import java.util.concurrent.ThreadPoolExecutor;

// @Async 비동기 메서드 실행을 위한 스레드 풀 설정 (재사용/자원 제한/큐잉을 위해 ThreadPoolTaskExecutor 사용)
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    // 알림 처리 전용 비동기 Executor (core 5 / max 20 / queue 100)
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("notification-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // 큐가 꽉 찼을 때 호출자 스레드에서 직접 실행 (알림 유실 방지)
        // AbortPolicy(기본값)는 RejectedExecutionException을 던져 알림이 사라짐
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // MDC TaskDecorator 등록: 부모 스레드의 MDC 컨텍스트를 자식 스레드에 전파
        // @Async로 실행되는 비동기 메서드에서도 requestId가 로그에 출력되도록 보장
        executor.setTaskDecorator(new MdcTaskDecorator());

        // 스레드 풀 초기화
        executor.initialize();

        log.debug("알림 비동기 처리 ThreadPoolTaskExecutor 초기화 완료 - core: {}, max: {}, queue: 100",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

     // 기본 비동기 Executor 설정
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

    // 비동기 예외 처리기 — @Async는 호출자에게 예외를 전파하지 않으므로 여기서 로깅
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    // MDC 컨텍스트를 비동기 스레드로 전파 — 새 스레드는 부모의 requestId를 자동 상속하지 않으므로 직접 복사
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

    // 비동기 메서드에서 발생한 예외를 로깅하는 핸들러
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