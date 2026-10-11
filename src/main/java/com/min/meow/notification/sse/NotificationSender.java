package com.min.meow.notification.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.min.meow.notification.dto.response.NotificationResponse;
import com.min.meow.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void send(Notification notification) {
        try {
            String channel = "sse:notify:" + notification.getReceiverUserId();
            String payload = objectMapper.writeValueAsString(NotificationResponse.from(notification));
            redisTemplate.convertAndSend(channel, payload);
            log.debug("[Redis Pub/Sub] 알림 발행 완료 - receiverUserId: {}", notification.getReceiverUserId());
        } catch (JsonProcessingException e) {
            log.error("[Redis Pub/Sub] 알림 직렬화 실패 - receiverUserId: {}", notification.getReceiverUserId(), e);
        }
    }
}
