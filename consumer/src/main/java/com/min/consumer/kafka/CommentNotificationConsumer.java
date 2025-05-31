package com.min.consumer.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.kafka.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.min.consumer.service.CommentNotificationService;

@Component
@RequiredArgsConstructor
public class CommentNotificationConsumer {

    private final CommentNotificationService commentNotificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "comment-notification", groupId = "meow")
    public void listen(String message) {
        try {
            NotificationDto noticeDto = objectMapper.readValue(message, NotificationDto.class);

            commentNotificationService.commentNotice(noticeDto);
            System.out.println("알림 수신: " + noticeDto);

        } catch (Exception e) {
            System.err.println("Kafka 메시지 역직렬화 실패: " + e.getMessage());
        }
    }
}