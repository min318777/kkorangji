package com.min.meow.notification.event;

import com.min.meow.notification.entity.Notification;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 📌 투두: 4단계 연결 - 알림 저장 완료 이벤트
 * 역할: 알림이 DB에 저장되었음을 알리는 Spring Event
 * 왜 필요한가?
 * - 도메인 모듈은 API 모듈에 의존하지 않음 (SSE 기능 접근 불가)
 * - Spring Event를 통해 모듈 간 통신
 * - 도메인: "알림 저장 완료!" 이벤트 발행
 * - API: 이벤트 수신 → SSE로 실시간 전송
 * 흐름:
 * 1. 도메인의 NotificationService가 알림 저장
 * 2. 이 이벤트 발행 (ApplicationEventPublisher 사용)
 * 3. API 모듈의 Listener가 이벤트 수신
 * 4. SSE로 실시간 푸시
 */
@Getter
public class NotificationSavedEvent extends ApplicationEvent {

    private final Notification notification;

    /**
     * 이벤트 생성자
     * @param source 이벤트를 발생시킨 객체 (일반적으로 Service)
     * @param notification 저장된 알림 엔티티
     */
    public NotificationSavedEvent(Object source, Notification notification) {
        super(source);
        this.notification = notification;
    }
}
