package com.min.meow.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.notice.NoticeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class KafkaMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;


    public void send(String topic, NoticeDto noticeDto) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(noticeDto);
            kafkaTemplate.send(topic, jsonMessage);
        } catch (JsonProcessingException e) {
            // 로깅 또는 예외 처리 (실무에서는 로그 필수)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
    }

}
