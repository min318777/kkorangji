package com.min.kafka.producer;

import com.min.kafka.dto.NotificationDto;
import com.min.meow.global.NotificationType;
import com.min.meow.notification.event.CommentCreateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final KafkaTemplate<String, NotificationDto> kafkaTemplate;

    public void send(String topic, NotificationDto notificationDto) {
        kafkaTemplate.send(topic, notificationDto);
    }

    public void publish(CommentCreateEvent event){
        NotificationDto dto = NotificationDto.builder()
                .sourceId(event.commentId())
                .postId(event.postId())
                .type(NotificationType.COMMENT)
                .isRead(false)
                .receiverLoginId(event.receiverLoginId())
                .message("댓글이 달렸습니다.")
                .build();
        kafkaTemplate.send("comment-notification", dto);
    }
}
