package com.min.meow.notification.event;

import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.service.NotificationSaveService;
import com.min.meow.notification.sse.NotificationSender;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 알림 이벤트 리스너 — 댓글/좋아요 트랜잭션이 AFTER_COMMIT된 후에만 notificationExecutor 스레드에서 비동기로 알림 저장+SSE 전송
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationSaveService notificationSaveService;
    private final NotificationSender notificationSender;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handle(NotificationEvent event) {
        log.debug("알림 이벤트 수신 - type: {}, sourceId: {}, postId: {}, receiverUserId: {}",
                event.type(), event.sourceId(), event.postId(), event.receiverUserId());

        Notification notification = Notification.builder()
                .sourceId(event.sourceId())
                .postId(event.postId())
                .postType(event.postType())
                .receiverUserId(event.receiverUserId())
                .type(event.type())
                .message(event.message())
                .isRead(false)
                .build();

        Notification saved = notificationSaveService.save(notification);
        if (saved == null) return;
        log.debug("DB 알림 저장 완료 - ID: {}, Type: {}, ReceiverUserId: {}",
                saved.getId(), event.type(), saved.getReceiverUserId());

        notificationSender.send(saved);
    }

    @Recover
    public void recover(Exception e, NotificationEvent event) {
        log.error("알림 최종 실패 (3회 재시도 소진) - type: {}, sourceId: {}, error: {}",
                event.type(), event.sourceId(), e.getMessage());
        Sentry.captureException(e);
    }
}
