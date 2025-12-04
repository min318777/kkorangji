package com.min.kafka.consumer;


import com.min.kafka.dto.NotificationDto;
import com.min.meow.notification.CreateNotificationCommand;
import com.min.meow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "comment-notification", groupId = "meow")
    public void listenComment(NotificationDto notificationDto) {
        CreateNotificationCommand command = new CreateNotificationCommand(
                notificationDto.getSourceId(),
                notificationDto.getPostId(),
                notificationDto.getReceiverLoginId()
        );
        notificationService.saveNotification(command);
        System.out.println("댓글 알림 수신: " + notificationDto);
    }


    @KafkaListener(topics = "like-notification", groupId = "meow")
    public void listenLike(NotificationDto notificationDto) {
        CreateNotificationCommand command = new CreateNotificationCommand(
                notificationDto.getSourceId(),
                notificationDto.getPostId(),
                notificationDto.getReceiverLoginId()
        );
        notificationService.saveNotification(command);
        System.out.println("좋아요 알림 수신:" + notificationDto);
    }
}
