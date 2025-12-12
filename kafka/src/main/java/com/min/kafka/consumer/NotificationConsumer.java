package com.min.kafka.consumer;


import com.min.kafka.dto.NotificationDto;
import com.min.meow.global.NotificationType;
import com.min.meow.notification.CreateNotificationCommand;
import com.min.meow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 📌 투두: 3단계 연결 - Kafka Consumer
 * 역할: Kafka에서 알림 메시지를 받아서 처리
 * 왜 필요한가?
 * - 댓글이나 좋아요가 달리면 Kafka에 메시지가 발행됨
 * - 이 클래스가 메시지를 받아서 도메인 서비스 호출
 * - 도메인 서비스가 DB 저장 + 이벤트 발행 → API 모듈이 SSE 전송
 * 흐름:
 * 1. Kafka에서 "comment-notification" 또는 "like-notification" 토픽 메시지 수신
 * 2. CreateNotificationCommand 생성 (타입, 메시지 포함)
 * 3. 도메인의 NotificationService.saveNotification() 호출
 * 4. → 도메인: DB 저장 + 이벤트 발행
 * 5. → API 모듈: 이벤트 수신 + SSE 실시간 전송!
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "comment-notification", groupId = "meow")
    public void listenComment(NotificationDto notificationDto) {
        log.info("📨 Kafka에서 댓글 알림 수신: {}", notificationDto);
        // CreateNotificationCommand 생성
        CreateNotificationCommand command = new CreateNotificationCommand(
                notificationDto.getSourceId(),
                notificationDto.getPostId(),
                notificationDto.getReceiverLoginId(),
                NotificationType.COMMENT,
                "새로운 댓글이 달렸습니다."
        );

        notificationService.saveNotification(command);
    }

    /**
     * 좋아요 알림 처리
     */
    @KafkaListener(topics = "like-notification", groupId = "meow")
    public void listenLike(NotificationDto notificationDto) {
        log.info("📨 Kafka에서 좋아요 알림 수신: {}", notificationDto);

        CreateNotificationCommand command = new CreateNotificationCommand(
                notificationDto.getSourceId(),
                notificationDto.getPostId(),
                notificationDto.getReceiverLoginId(),
                NotificationType.LIKE,
                "게시글에 좋아요가 추가되었습니다."
        );
        notificationService.saveNotification(command);
    }
}
