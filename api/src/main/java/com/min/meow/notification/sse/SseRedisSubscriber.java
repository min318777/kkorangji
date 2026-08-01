package com.min.meow.notification.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.min.meow.notification.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 구독자 — SSE 알림 전달
 * 채널 패턴: sse:notify:{userId}
 * 모든 서버 인스턴스가 동일 패턴을 구독하며,
 * 자신의 ConcurrentHashMap에 해당 userId 연결이 있을 때만 SSE 전송
 */
@Slf4j
@Component
public class SseRedisSubscriber implements MessageListener {

    private final SseEmitterManager sseEmitterManager;
    // RedisConfig ObjectMapper는 activateDefaultTyping이 적용되어 타입 정보가 포함된 JSON을 생성함
    // Pub/Sub 역직렬화는 순수 ObjectMapper로 분리하여 타입 충돌 방지
    private final ObjectMapper objectMapper;

    public SseRedisSubscriber(SseEmitterManager sseEmitterManager) {
        this.sseEmitterManager = sseEmitterManager;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 채널명에서 userId 추출 (sse:notify:{userId})
            String channel = new String(message.getChannel());
            Long userId = Long.parseLong(channel.replace("sse:notify:", ""));

            // JSON → NotificationResponse 역직렬화
            NotificationResponse data = objectMapper.readValue(message.getBody(), NotificationResponse.class);

            // 이 서버에 해당 userId SSE 연결이 있으면 전송, 없으면 무시
            sseEmitterManager.sendToUser(userId, data);
            log.debug("[Redis Pub/Sub] SSE 전송 완료 - userId: {}", userId);

        } catch (Exception e) {
            log.error("[Redis Pub/Sub] 메시지 처리 실패 - channel: {}", new String(message.getChannel()), e);
        }
    }
}
