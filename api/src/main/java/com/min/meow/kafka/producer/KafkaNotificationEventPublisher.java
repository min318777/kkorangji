package com.min.meow.kafka.producer;

import com.min.meow.notification.event.CommentEvent;
import com.min.meow.notification.event.LikeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 알림 이벤트 Kafka 발행자
 * - CommentEvent, LikeEvent를 Kafka로 전송
 * - 현재 Spring Event 방식(NotificationEventPublisher)을 사용 중
 * - 추후 분산 환경 전환 시 이 클래스 활용 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNotificationEventPublisher {

    private final KafkaTemplate<String, CommentEvent> commentEventKafkaTemplate;
    private final KafkaTemplate<String, LikeEvent> likeEventKafkaTemplate;

    /**
     * 댓글 이벤트 발행
     */
    public void publishCommentEvent(CommentEvent event) {
        log.info("댓글 알림 발행 - commentId: {}, postId: {}, receiver: {}",
                event.commentId(), event.postId(), event.receiverLoginId());

        commentEventKafkaTemplate.send("comment-notification", event);
    }

    /**
     * 좋아요 이벤트 발행
     */
    public void publishLikeEvent(LikeEvent event) {
        log.info("좋아요 알림 발행 - likeId: {}, postId: {}, receiver: {}",
                event.likeId(), event.postId(), event.receiverLoginId());

        likeEventKafkaTemplate.send("like-notification", event);
    }
}
