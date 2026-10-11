package com.min.meow.notification.event;

import com.min.meow.notification.event.CommentEvent;
import com.min.meow.notification.event.LikeEvent;
import com.min.meow.notification.event.PopularScoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

// 알림 이벤트 발행자 — Spring Event를 발행하면 NotificationEventListener가 비동기로 수신해 알림 저장/SSE 전송
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    // 댓글 이벤트 발행
    public void publishCommentEvent(CommentEvent event) {
        log.debug("댓글 알림 이벤트 발행 - commentId: {}, postId: {}, receiverUserId: {}",
                event.commentId(), event.postId(), event.receiverUserId());

        eventPublisher.publishEvent(event);
        log.debug("댓글 알림 이벤트 발행 완료");
    }

    // 좋아요 이벤트 발행
    public void publishLikeEvent(LikeEvent event) {
        log.debug("좋아요 알림 이벤트 발행 - likeId: {}, postId: {}, receiverUserId: {}",
                event.likeId(), event.postId(), event.receiverUserId());

        eventPublisher.publishEvent(event);
        log.debug("좋아요 알림 이벤트 발행 완료");
    }

    // 인기글 점수 변경 이벤트 발행 — 좋아요(±3)/댓글(±2) 변경 시 호출, PopularRankingService가 Sorted Set ZINCRBY
    public void publishPopularScoreEvent(PopularScoreEvent event) {
        eventPublisher.publishEvent(event);
    }
}
