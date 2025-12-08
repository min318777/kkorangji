package com.min.meow.notification.event;

import com.min.meow.notification.dto.NotificationResponse;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 📌 투두: 5단계 최종 - 알림 이벤트 리스너 (SSE 실시간 전송)
 * 역할: 도메인 모듈에서 발행된 NotificationSavedEvent를 받아서 SSE로 실시간 전송
 * 왜 필요한가?
 * - 도메인 모듈은 API 모듈의 SSE 기능을 직접 사용할 수 없음 (모듈 의존성)
 * - Spring Event를 통해 모듈 간 통신
 * - 도메인이 "알림 저장 완료" 이벤트 발행 → 이 클래스가 수신 → SSE 전송
 * 흐름:
 * 1. 도메인의 NotificationService가 알림 저장 후 이벤트 발행
 * 2. 이 클래스의 @EventListener 메서드가 자동 호출
 * 3. 알림 받을 사람이 SSE 연결되어 있는지 확인
 * 4. 연결되어 있으면 즉시 SSE로 실시간 전송!
 * 5. 연결 안 되어 있으면 로그만 남김 (DB에는 이미 저장됨)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SseEmitterManager sseEmitterManager;

    /**
     * 알림 저장 이벤트 처리 → SSE 실시간 전송
     * @param event 도메인 모듈에서 발행된 NotificationSavedEvent
     */
    @EventListener
    public void handleNotificationSaved(NotificationSavedEvent event) {
        // 1. 이벤트에서 저장된 알림 엔티티 가져오기
        Notification notification = event.getNotification();
        log.info("📨 알림 저장 이벤트 수신: ID={}, 받는사람={}",
                notification.getId(), notification.getReceiverLoginId());

        // 2. DTO로 변환 (프론트엔드에 보낼 형태)
        NotificationResponse response = NotificationResponse.from(notification);

        // 3. 받을 사람의 로그인 ID
        String receiverLoginId = notification.getReceiverLoginId();

        // 4. SSE 연결 확인 후 실시간 전송
        if (sseEmitterManager.isConnected(receiverLoginId)) {
            // 연결되어 있음 → 즉시 SSE로 전송!
            sseEmitterManager.sendToUser(receiverLoginId, response);
            log.info("📬 실시간 알림 전송 성공: {}", receiverLoginId);
        } else {
            // 연결 안 되어 있음 → DB에만 저장되어 있음 (나중에 조회 가능)
            log.info("💤 사용자 미접속, DB에만 저장됨: {}", receiverLoginId);
        }
    }
}
