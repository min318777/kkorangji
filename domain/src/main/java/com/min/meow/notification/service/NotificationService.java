package com.min.meow.notification.service;

import com.min.meow.notification.CreateNotificationCommand;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.event.NotificationSavedEvent;
import com.min.meow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 📌 투두: 3단계 - 알림 저장 및 이벤트 발행
 *
 * 역할: Kafka Consumer에서 호출되어 알림을 DB에 저장하고 이벤트 발행
 *
 * 왜 필요한가?
 * - 도메인 모듈이므로 SSE 기능에 직접 접근할 수 없음
 * - Spring Event를 발행하여 API 모듈에 알림 저장 완료를 알림
 * - API 모듈의 Listener가 이벤트를 받아 SSE 실시간 전송 처리
 *
 * 흐름:
 * 1. Kafka Consumer가 이 메서드 호출
 * 2. Notification 엔티티 생성 (type, message 포함)
 * 3. DB에 저장
 * 4. NotificationSavedEvent 발행
 * 5. API 모듈이 이벤트 수신 → SSE 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void saveNotification(CreateNotificationCommand command){

        // 1. Notification 엔티티 생성 (type, message 포함)
        Notification notification = Notification.builder()
                .sourceId(command.sourceId())
                .postId(command.postId())
                .receiverLoginId(command.receiverLoginId())
                .type(command.type())
                .message(command.message())
                .isRead(false)
                .build();

        // 2. DB에 저장
        Notification saved = notificationRepository.save(notification);
        log.info("💾 알림 저장 완료: ID={}, 타입={}, 받는사람={}",
                saved.getId(), saved.getType(), saved.getReceiverLoginId());

        // 3. Spring Event 발행 (API 모듈의 Listener가 수신)
        eventPublisher.publishEvent(new NotificationSavedEvent(this, saved));
        log.info("📢 알림 저장 이벤트 발행: {}", saved.getId());
    }
}
