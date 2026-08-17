package com.min.meow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 스케줄링 설정 클래스
 * Spring의 @Scheduled 어노테이션을 사용한 스케줄 작업 실행을 위한 설정입니다.
 * ThreadPoolTaskScheduler를 사용하여 스케줄 스레드 풀을 관리합니다.
 * 주요 스케줄 작업:
 * - Redis 조회수 → DB 동기화 (30초마다)
 * - 기타 주기적 배치 작업
 * @EnableScheduling: Spring의 스케줄링 기능을 활성화합니다.
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * 스케줄링 전용 ThreadPoolTaskScheduler
     * 설정값 설명:
     * - poolSize: 스케줄러 스레드 풀 크기 (4개)
     *   → 등록된 스케줄 작업 3개(조회수 동기화, SSE 하트비트, 인기글 v5 캐시 워밍) + 여유분 1개
     * - threadNamePrefix: 스레드 이름 접두사
     *   → 로그에서 스케줄 작업 스레드를 쉽게 식별
     * - waitForTasksToCompleteOnShutdown: 종료 시 작업 완료 대기
     *   → 애플리케이션 종료 시 진행 중인 스케줄 작업 완료 보장
     * - awaitTerminationSeconds: 종료 대기 시간 (30초)
     *   → 최대 30초까지 작업 완료를 기다림
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // 스케줄러 스레드 풀 크기
        // 등록된 스케줄 작업 3개(조회수 동기화, SSE 하트비트, 인기글 v5 캐시 워밍) + 여유분 1개
        scheduler.setPoolSize(4);

        // 스레드 이름 접두사: 로그에서 식별 용이
        scheduler.setThreadNamePrefix("scheduler-");

        // 애플리케이션 종료 시 진행 중인 작업 완료까지 대기
        scheduler.setWaitForTasksToCompleteOnShutdown(true);

        // 종료 시 최대 대기 시간 (초)
        scheduler.setAwaitTerminationSeconds(30);

        // 스케줄러 초기화
        scheduler.initialize();

        log.info("스케줄링 ThreadPoolTaskScheduler 초기화 완료");
        log.info("   - Pool Size: {}", scheduler.getPoolSize());

        return scheduler;
    }
}


