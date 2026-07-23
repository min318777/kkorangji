package com.min.meow.notification.event;

import com.min.meow.common.NotificationType;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.service.NotificationSaveService;
import com.min.meow.notification.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 이벤트 리스너 (비동기 처리)
 * Spring Event를 수신하여 알림을 처리하는 리스너입니다.
 * 처리 흐름:
 * 1. CommentEvent 또는 LikeEvent를 트랜잭션 커밋 후(AFTER_COMMIT) 수신
 * 2. Notification 엔티티 생성
 * 3. DB에 알림 저장
 * 4. SSE를 통해 실시간 알림 전송
 * @TransactionalEventListener(AFTER_COMMIT) 설명:
 * - 발행자(댓글/좋아요 등록) 트랜잭션이 정상 커밋된 후에만 리스너 실행
 * - 트랜잭션 롤백 시 리스너 미실행 → 댓글/좋아요와 알림 간 정합성 보장
 * - 일반 @EventListener는 publishEvent 호출 즉시 실행되어 롤백 시 알림이 잘못 발송될 위험이 있음
 * @Async 설명:
 * - "notificationExecutor": AsyncConfig에서 정의한 스레드 풀 사용
 * - 이벤트 발행자(Service)는 블로킹 없이 즉시 반환
 * - 알림 처리는 별도 스레드에서 비동기로 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationSaveService notificationSaveService;
    private final SseEmitterManager sseEmitterManager;

    /**
     * 댓글 이벤트 처리
     * @param event 댓글 이벤트 정보
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentEvent(CommentEvent event) {
        log.debug("댓글 이벤트 수신 - commentId: {}, postId: {}, receiverUserId: {}",
                event.commentId(), event.postId(), event.receiverUserId());
        try {
            Notification notification = Notification.builder()
                    .sourceId(event.commentId())
                    .postId(event.postId())
                    .postType(event.postType())
                    .receiverUserId(event.receiverUserId())
                    .type(NotificationType.COMMENT)
                    .message(createCommentNotificationMessage(event.writer()))
                    .isRead(false)
                    .build();

            Notification saved = notificationSaveService.save(notification);
            if (saved == null) return;
            log.debug("DB 알림 저장 완료 - ID: {}, Type: COMMENT, ReceiverUserId: {}",
                    saved.getId(), saved.getReceiverUserId());

            sendSseNotification(saved);

        } catch (Exception e) {
            log.error("댓글 알림 처리 실패 - commentId: {}, error: {}",
                    event.commentId(), e.getMessage(), e);
        }
    }

    /**
     * 좋아요 이벤트 처리
     * @param event 좋아요 이벤트 정보
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeEvent(LikeEvent event) {
        log.debug("좋아요 이벤트 수신 - likeId: {}, postId: {}, receiverUserId: {}",
                event.likeId(), event.postId(), event.receiverUserId());

        try {
            Notification notification = Notification.builder()
                    .sourceId(event.likeId())
                    .postId(event.postId())
                    .postType(event.postType())
                    .receiverUserId(event.receiverUserId())
                    .type(NotificationType.LIKE)
                    .message("게시글에 좋아요가 추가되었습니다.")
                    .isRead(false)
                    .build();

            Notification saved = notificationSaveService.save(notification);
            if (saved == null) return;
            log.debug("DB 알림 저장 완료 - ID: {}, Type: LIKE, ReceiverUserId: {}",
                    saved.getId(), saved.getReceiverUserId());

            sendSseNotification(saved);

        } catch (Exception e) {
            log.error("좋아요 알림 처리 실패 - likeId: {}, error: {}",
                    event.likeId(), e.getMessage(), e);
        }
    }

    /**
     * SSE 실시간 알림 전송
     * @param notification 저장된 알림 엔티티
     */
    private void sendSseNotification(Notification notification) {
        Long receiverUserId = notification.getReceiverUserId();

        if (sseEmitterManager.isConnected(receiverUserId)) {
            sseEmitterManager.sendToUser(receiverUserId, notification);
            log.debug("SSE 실시간 알림 전송 완료 - ReceiverUserId: {}", receiverUserId);
        } else {
            log.debug("[SSE] 사용자 미접속 - ReceiverUserId: {} (DB 저장 완료, 나중에 조회 가능)",
                    receiverUserId);
        }
    }

    /**
     * 댓글 알림 메시지 생성
     */
    private String createCommentNotificationMessage(String writer) {
        return String.format("%s님이 댓글을 남겼습니다.", writer);
    }
}
