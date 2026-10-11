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

// 알림 DB 저장 전용 서비스 — @Async+@Transactional을 한 클래스에 쓰면 프록시 한계로 트랜잭션이 안 먹혀서 분리됨
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSaveService {

    private final NotificationRepository notificationRepository;

    // 알림 저장 — DataAccessException 발생 시 최대 3회 재시도, 지수 백오프
    @Transactional
    @Retryable(
            retryFor = DataAccessException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    // 3회 재시도 후 최종 실패 시 호출 — 알림 유실을 로그/Sentry로 남김
    @Recover
    public Notification recover(DataAccessException e, Notification notification) {
        log.error("알림 저장 최종 실패 (3회 재시도 소진) - type: {}, receiverUserId: {}, error: {}",
                notification.getType(), notification.getReceiverUserId(), e.getMessage());
        Sentry.captureException(e);
        return null;
    }
}
