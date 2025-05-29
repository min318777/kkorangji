package com.min.meow.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.notice.NoticeDto;
import com.min.meow.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaMessageListener {

    private final NoticeService noticeService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "lost-cat-topic", groupId = "meow")
    public void listen(String message) {
        try {
            NoticeDto noticeDto = objectMapper.readValue(message, NoticeDto.class);

            noticeService.saveNotice(noticeDto);
            System.out.println("알림 수신: " + noticeDto);

        } catch (Exception e) {
            System.err.println("Kafka 메시지 역직렬화 실패: " + e.getMessage());
        }
    }
}
