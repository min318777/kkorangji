package com.min.meow.notification.kafka.consumer;

import com.min.meow.global.NotificationType;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.event.CommentEvent;
import com.min.meow.notification.event.LikeEvent;
import com.min.meow.notification.repository.NotificationRepository;
import com.min.meow.notification.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka Consumer - 알림 이벤트 수신 및 처리
 * 역할:
 * 1. Kafka에서 CommentEvent, LikeEvent 수신
 * 2. DB에 알림 저장
 * 3. SSE를 통해 실시간 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final SseEmitterManager sseEmitterManager;

    /**
     * 댓글 이벤트 수신
     */
    @Transactional
    @KafkaListener(
            topics = "comment-notification",
            groupId = "notification-service",
            containerFactory = "commentEventKafkaListenerContainerFactory"
    )
    public void listenCommentEvent(CommentEvent event) {
        log.info("[Kafka] 댓글 이벤트 수신 - commentId: {}, postId: {}, receiver: {}",
                event.commentId(), event.postId(), event.receiverLoginId());

        // 1. 알림 엔티티 생성 및 저장
        Notification notification = Notification.builder()
                .sourceId(event.commentId())
                .postId(event.postId())
                .receiverLoginId(event.receiverLoginId())
                .type(NotificationType.COMMENT)
                .message("새로운 댓글이 달렸습니다.")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("[DB] 알림 저장 완료 - ID: {}, Type: COMMENT, Receiver: {}",
                saved.getId(), saved.getReceiverLoginId());

        // 2. SSE 실시간 전송
        sseEmitterManager.sendToUser(saved.getReceiverLoginId(), saved);
        log.info("[SSE] 실시간 알림 전송 완료 - Receiver: {}", saved.getReceiverLoginId());
    }

    /**
     * 좋아요 이벤트 수신
     */
    @Transactional
    @KafkaListener(
            topics = "like-notification",
            groupId = "notification-service",
            containerFactory = "likeEventKafkaListenerContainerFactory"
    )
    public void listenLikeEvent(LikeEvent event) {
        log.info("[Kafka] 좋아요 이벤트 수신 - likeId: {}, postId: {}, receiver: {}",
                event.likeId(), event.postId(), event.receiverLoginId());

        // 1. 알림 엔티티 생성 및 저장
        Notification notification = Notification.builder()
                .sourceId(event.likeId())
                .postId(event.postId())
                .receiverLoginId(event.receiverLoginId())
                .type(NotificationType.LIKE)
                .message("게시글에 좋아요가 추가되었습니다.")
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("[DB] 알림 저장 완료 - ID: {}, Type: LIKE, Receiver: {}",
                saved.getId(), saved.getReceiverLoginId());

        // 2. SSE 실시간 전송
        sseEmitterManager.sendToUser(saved.getReceiverLoginId(), saved);
        log.info("[SSE] 실시간 알림 전송 완료 - Receiver: {}", saved.getReceiverLoginId());
    }
}
