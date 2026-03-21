package com.min.meow.notification.event;

import com.min.meow.global.NotificationType;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.repository.NotificationRepository;
import com.min.meow.notification.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 이벤트 리스너 (비동기 처리)
 * Spring Event를 수신하여 알림을 처리하는 리스너입니다.
 * 기존 Kafka Consumer의 역할을 대체합니다.
 * 처리 흐름:
 * 1. CommentEvent 또는 LikeEvent 수신
 * 2. Notification 엔티티 생성
 * 3. DB에 알림 저장
 * 4. SSE를 통해 실시간 알림 전송
 * @Async 설명:
 * - "notificationExecutor": AsyncConfig에서 정의한 스레드 풀 사용
 * - 이벤트 발행자(Service)는 블로킹 없이 즉시 반환
 * - 알림 처리는 별도 스레드에서 비동기로 실행
 * 장점:
 * 1. API 응답 시간 단축: 알림 처리가 API 응답을 지연시키지 않음
 * 2. 장애 격리: 알림 처리 실패가 메인 비즈니스 로직에 영향 없음
 * 3. 확장성: 스레드 풀 크기 조절로 처리량 조절 가능
 * 주의사항:
 * - @Async와 @Transactional을 함께 사용할 때 트랜잭션은 새로 시작됨
 * - 비동기 메서드에서 발생한 예외는 호출자에게 전파되지 않음
 * - AsyncConfig의 AsyncUncaughtExceptionHandler에서 예외 처리됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final SseEmitterManager sseEmitterManager;

    /**
     * 댓글 이벤트 처리
     * 1. 이벤트 정보 로깅
     * 2. Notification 엔티티 생성 (COMMENT 타입)
     * 3. DB 저장
     * 4. SSE로 실시간 전송 (사용자가 접속 중인 경우)
     * @param event 댓글 이벤트 정보
     */
    @Async("notificationExecutor")
    @EventListener
    @Transactional
    public void handleCommentEvent(CommentEvent event) {
        log.debug("댓글 이벤트 수신 - commentId: {}, postId: {}, receiver: {}",
                event.commentId(), event.postId(), event.receiverLoginId());
        try {
            // 1. 알림 엔티티 생성
            Notification notification = Notification.builder()
                    .sourceId(event.commentId())
                    .postId(event.postId())
                    .receiverLoginId(event.receiverLoginId())
                    .type(NotificationType.COMMENT)
                    .message(createCommentNotificationMessage(event.writer()))
                    .isRead(false)
                    .build();

            // 2. DB에 저장
            Notification saved = notificationRepository.save(notification);
            log.debug("DB 알림 저장 완료 - ID: {}, Type: COMMENT, Receiver: {}",
                    saved.getId(), saved.getReceiverLoginId());

            // 3. SSE 실시간 전송 (사용자가 접속 중인 경우에만)
            sendSseNotification(saved);

        } catch (Exception e) {
            // 알림 처리 실패는 메인 비즈니스 로직에 영향을 주지 않도록 로깅만 수행
            log.error("댓글 알림 처리 실패 - commentId: {}, error: {}",
                    event.commentId(), e.getMessage(), e);
        }
    }

    /**
     * 좋아요 이벤트 처리
     * 게시글에 좋아요가 추가되면 이 메서드가 비동기로 호출됩니다.
     * 처리 순서:
     * 1. 이벤트 정보 로깅
     * 2. Notification 엔티티 생성 (LIKE 타입)
     * 3. DB 저장
     * 4. SSE로 실시간 전송 (사용자가 접속 중인 경우)
     * @param event 좋아요 이벤트 정보
     */
    @Async("notificationExecutor")
    @EventListener
    @Transactional
    public void handleLikeEvent(LikeEvent event) {
        log.debug("좋아요 이벤트 수신 - likeId: {}, postId: {}, receiver: {}",
                event.likeId(), event.postId(), event.receiverLoginId());

        try {
            // 1. 알림 엔티티 생성
            Notification notification = Notification.builder()
                    .sourceId(event.likeId())
                    .postId(event.postId())
                    .receiverLoginId(event.receiverLoginId())
                    .type(NotificationType.LIKE)
                    .message("게시글에 좋아요가 추가되었습니다.")
                    .isRead(false)
                    .build();

            // 2. DB에 저장
            Notification saved = notificationRepository.save(notification);
            log.debug("DB 알림 저장 완료 - ID: {}, Type: LIKE, Receiver: {}",
                    saved.getId(), saved.getReceiverLoginId());

            // 3. SSE 실시간 전송 (사용자가 접속 중인 경우에만)
            sendSseNotification(saved);

        } catch (Exception e) {
            // 알림 처리 실패는 메인 비즈니스 로직에 영향을 주지 않도록 로깅만 수행
            log.error("좋아요 알림 처리 실패 - likeId: {}, error: {}",
                    event.likeId(), e.getMessage(), e);
        }
    }

    /**
     * SSE 실시간 알림 전송
     * 사용자가 SSE로 연결되어 있는 경우에만 실시간 알림을 전송합니다.
     * 연결되어 있지 않으면 DB에만 저장되고 나중에 조회할 수 있습니다.
     * @param notification 저장된 알림 엔티티
     */
    private void sendSseNotification(Notification notification) {
        String receiverLoginId = notification.getReceiverLoginId();

        // 사용자 연결 상태 확인 후 전송
        if (sseEmitterManager.isConnected(receiverLoginId)) {
            sseEmitterManager.sendToUser(receiverLoginId, notification);
            log.debug("SSE 실시간 알림 전송 완료 - Receiver: {}", receiverLoginId);
        } else {
            log.debug("[SSE] 사용자 미접속 - Receiver: {} (DB 저장 완료, 나중에 조회 가능)",
                    receiverLoginId);
        }
    }

    /**
     * 댓글 알림 메시지 생성
     * 댓글 작성자 정보를 포함한 알림 메시지를 생성합니다.
     * @param writer 댓글 작성자
     * @return 알림 메시지
     */
    private String createCommentNotificationMessage(String writer) {
        return String.format("%s님이 댓글을 남겼습니다.", writer);
    }
}