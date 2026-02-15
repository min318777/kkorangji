package com.min.meow.post.postlike.scheduler;

import com.min.meow.post.postlike.service.LikeCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis 좋아요 → DB 동기화 스케줄러
 *
 * 주기적으로 Redis에 저장된 좋아요 변경분을 DB에 반영합니다.
 *
 * 동작 방식:
 * ┌─────────────┐   SADD/SREM   ┌─────────────┐    Batch    ┌─────────────┐
 * │   Client    │ ────────────▶ │    Redis    │ ─────────▶  │   MySQL     │
 * │  (Request)  │               │  (SET/INCR) │  (1분마다)  │    (DB)     │
 * └─────────────┘               └─────────────┘             └─────────────┘
 *
 * 동기화 주기 설정:
 * - 기본값: 1분 (60,000ms)
 * - 너무 짧으면: DB 부하 증가, Redis의 장점 감소
 * - 너무 길면: 서버 장애 시 데이터 손실 증가, 실시간성 감소
 *
 * 동기화 내용:
 * 1. like:delta:{postType}:{postId} → likeCount 컬럼에 원자적 UPDATE
 * 2. like:pending:{postType} → 변경된 게시글 목록 처리 후 초기화
 *
 * 운영 고려사항:
 * 1. 장애 복구: 동기화 실패 시 다음 주기에 재시도
 * 2. 데이터 정합성: 동기화 후 delta 키 삭제
 * 3. 모니터링: 동기화 결과 로깅 (성공 건수, 소요 시간)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeSyncScheduler {

    private final LikeCountService likeCountService;

    /**
     * Redis 좋아요를 DB에 동기화 (1분마다 실행)
     *
     * @Scheduled 설정:
     * - fixedRate: 이전 실행 시작 시점으로부터 지정 시간 후 실행
     * - initialDelay: 애플리케이션 시작 후 첫 실행까지 대기 시간
     *   → 애플리케이션 초기화 완료 후 실행하기 위해 60초 대기
     *
     * 조회수 동기화와 30초 시차를 두어 DB 부하 분산
     * - 조회수: 0초, 60초, 120초, ...
     * - 좋아요: 30초, 90초, 150초, ...
     */
    @Scheduled(fixedRate = 60000, initialDelay = 90000)
    public void syncLikesToDatabase() {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("좋아요 동기화 스케줄 시작");

            likeCountService.syncLikesToDatabase();

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.debug("좋아요 동기화 스케줄 완료 - 소요시간: {}ms", elapsedTime);

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("좋아요 동기화 스케줄 실패 - 소요시간: {}ms, 에러: {}",
                    elapsedTime, e.getMessage(), e);
            // 예외를 던지지 않고 다음 스케줄에서 재시도
        }
    }
}
