package com.min.meow.notification.event;

import com.min.meow.notification.event.CommentEvent;
import com.min.meow.notification.event.LikeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 알림 이벤트 발행자 (Spring Event 기반)
 * 흐름:
 * 1. 서비스에서 댓글/좋아요 생성 시 이 클래스의 publish 메서드 호출
 * 2. ApplicationEventPublisher를 통해 Spring Event 발행
 * 3. NotificationEventListener가 이벤트 수신 (비동기)
 * 4. 알림 저장 및 SSE 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 댓글 이벤트 발행
     * @param event 댓글 이벤트 (댓글 ID, 게시글 ID, 작성자, 수신자 userId)
     */
    public void publishCommentEvent(CommentEvent event) {
        log.debug("댓글 알림 이벤트 발행 - commentId: {}, postId: {}, receiverUserId: {}",
                event.commentId(), event.postId(), event.receiverUserId());

        // Spring Event 발행 - 동기적으로 발행하지만, 리스너가 @Async로 비동기 처리
        eventPublisher.publishEvent(event);

        log.debug("댓글 알림 이벤트 발행 완료");
    }

    /**
     * 좋아요 이벤트 발행
     * @param event 좋아요 이벤트 (좋아요 ID, 게시글 ID, 수신자 userId)
     */
    public void publishLikeEvent(LikeEvent event) {
        log.debug("좋아요 알림 이벤트 발행 - likeId: {}, postId: {}, receiverUserId: {}",
                event.likeId(), event.postId(), event.receiverUserId());

        // Spring Event 발행 - 동기적으로 발행하지만, 리스너가 @Async로 비동기 처리
        eventPublisher.publishEvent(event);

        log.debug("좋아요 알림 이벤트 발행 완료");
    }
}
