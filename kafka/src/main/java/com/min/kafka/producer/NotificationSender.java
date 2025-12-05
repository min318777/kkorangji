package com.min.kafka.producer;

import com.min.kafka.dto.NotificationDto;
import com.min.meow.global.NotificationType;
import com.min.meow.notification.event.CommentEvent;
import com.min.meow.notification.event.LikeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final KafkaTemplate<String, NotificationDto> kafkaTemplate;

    public void publish(CommentEvent event){
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
    public void publish(LikeEvent event){
        NotificationDto dto = NotificationDto.builder()
                .sourceId(event.likeId())
                .postId(event.postId())
                .type(NotificationType.LIKE)
                .isRead(false)
                .receiverLoginId(event.receiverLoginId())
                .message("좋아요를 받았습니다.")
                .build();
        kafkaTemplate.send("like-notification", dto);
    }
}
