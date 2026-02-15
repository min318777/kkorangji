package com.min.meow.post.scheduler;

import com.min.meow.post.service.ViewCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis 조회수 → DB 동기화 스케줄러
 *
 * 주기적으로 Redis에 저장된 조회수 증가분을 DB에 반영합니다.
 *
 * 동작 방식:
 * ┌─────────────┐    INCR    ┌─────────────┐    Batch    ┌─────────────┐
 * │   Client    │ ─────────▶ │    Redis    │ ─────────▶  │   MySQL     │
 * │  (Request)  │            │   (Cache)   │  (1분마다)  │    (DB)     │
 * └─────────────┘            └─────────────┘             └─────────────┘
 *
 * 동기화 주기 설정:
 * - 기본값: 1분 (60,000ms)
 * - 너무 짧으면: DB 부하 증가, Redis의 장점 감소
 * - 너무 길면: 서버 장애 시 데이터 손실 증가, 실시간성 감소
 *
 * 운영 고려사항:
 * 1. 장애 복구: 동기화 실패 시 다음 주기에 재시도
 * 2. 데이터 정합성: 동기화 후 Redis 키 삭제
 * 3. 모니터링: 동기화 결과 로깅 (성공 건수, 소요 시간)
 *
 * 커스터마이징:
 * - application.yml에서 cron 표현식으로 스케줄 설정 가능
 * - 예: view.sync.cron: "0 * * * * *" (매분 0초)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {

    private final ViewCountService viewCountService;

    /**
     * Redis 조회수를 DB에 동기화 (1분마다 실행)
     *
     * @Scheduled 설정:
     * - fixedRate: 이전 실행 시작 시점으로부터 지정 시간 후 실행
     * - initialDelay: 애플리케이션 시작 후 첫 실행까지 대기 시간
     *   → 애플리케이션 초기화 완료 후 실행하기 위해 60초 대기
     *
     * 실행 시간이 fixedRate보다 길어지면:
     * - 다음 실행은 현재 실행 완료 직후 시작됨
     * - 동시 실행은 발생하지 않음 (기본 설정)
     *
     * 성능 최적화:
     * - 배치 UPDATE로 DB 쿼리 최소화
     * - 동기화 후 Redis 키 삭제하여 메모리 관리
     */
    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    public void syncViewCountsToDatabase() {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("조회수 동기화 스케줄 시작");

            viewCountService.syncViewCountsToDatabase();

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.debug("조회수 동기화 스케줄 완료 - 소요시간: {}ms", elapsedTime);

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("조회수 동기화 스케줄 실패 - 소요시간: {}ms, 에러: {}",
                    elapsedTime, e.getMessage(), e);
            // 예외를 던지지 않고 다음 스케줄에서 재시도
        }
    }
}
