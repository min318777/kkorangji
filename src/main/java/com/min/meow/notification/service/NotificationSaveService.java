package com.min.meow.notification.service;

import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.repository.NotificationRepository;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 DB 저장 전용 서비스
 * @Async + @Transactional을 같은 클래스에 쓰면 Spring 프록시 한계로 트랜잭션이 적용 안 됨.
 * NotificationEventListener(@Async) → NotificationSaveService(@Transactional)으로 분리해 해결.
 * @Retryable: DB 일시 장애 시 최대 3회 재시도 (1s → 2s 지수 백오프)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSaveService {

    private final NotificationRepository notificationRepository;

    /**
     * 알림 저장 (재시도 포함)
     * DataAccessException 발생 시 최대 3회 재시도, 지수 백오프 적용
     */
    @Transactional
    @Retryable(
            retryFor = DataAccessException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    /**
     * 3회 재시도 후 최종 실패 시 호출
     * 알림 유실을 로그로 남겨 모니터링 가능하도록 함
     */
    @Recover
    public Notification recover(DataAccessException e, Notification notification) {
        log.error("알림 저장 최종 실패 (3회 재시도 소진) - type: {}, receiverUserId: {}, error: {}",
                notification.getType(), notification.getReceiverUserId(), e.getMessage());
        Sentry.captureException(e);
        return null;
    }
}
