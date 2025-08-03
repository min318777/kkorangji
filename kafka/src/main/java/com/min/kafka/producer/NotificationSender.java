package com.min.kafka.producer;

import com.min.kafka.dto.NotificationDto;
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
}
