package com.min.meow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// @Scheduled 작업 실행을 위한 ThreadPoolTaskScheduler 설정 (현재 SseEmitterManager의 heartbeat가 사용)
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig {

    // 스케줄링 전용 ThreadPoolTaskScheduler (poolSize 4, 종료 시 진행 중 작업 최대 30초 대기)
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();

        log.info("스케줄링 ThreadPoolTaskScheduler 초기화 완료");
        log.info("   - Pool Size: {}", scheduler.getPoolSize());

        return scheduler;
    }
}



