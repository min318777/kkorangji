package com.min.meow.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.notification.domain.NotificationDto;
import com.min.meow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaMessageListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "lost-cat-topic", groupId = "meow")
    public void listen(String message) {
        try {
            NotificationDto notificationDto = objectMapper.readValue(message, NotificationDto.class);

            notificationService.saveNotification(notificationDto);
            System.out.println("알림 수신: " + notificationDto);

        } catch (Exception e) {
            System.err.println("Kafka 메시지 역직렬화 실패: " + e.getMessage());
        }
    }
}
