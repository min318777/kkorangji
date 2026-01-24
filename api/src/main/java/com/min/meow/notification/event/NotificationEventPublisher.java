package com.min.meow.notification.event;

import com.min.meow.notification.event.CommentEvent;
import com.min.meow.notification.event.LikeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 알림 이벤트 발행자 (Spring Event 기반)
 * 기존 Kafka 기반에서 Spring Event 기반으로 변경되었습니다.
 * 왜 Spring Event를 사용하는가?
 * 1. 단순성: 외부 인프라(Kafka) 없이 애플리케이션 내에서 이벤트 처리
 * 2. 낮은 지연시간: 네트워크 통신 없이 인메모리 처리
 * 3. 개발/테스트 용이성: 별도의 Kafka 설정 없이 동작
 * 4. 비동기 처리: @Async와 결합하여 비동기 처리 가능
 * 주의사항:
 * - 분산 환경에서 여러 인스턴스 간 이벤트 공유 불가
 * - 애플리케이션 재시작 시 미처리 이벤트 손실 가능
 * - 대용량 트래픽 시 Kafka로 전환 고려 필요
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
     * 새 댓글이 작성되면 이 메서드를 호출하여 알림 이벤트를 발행합니다.
     * 비동기 리스너가 이벤트를 수신하여 알림을 처리합니다.
     * @param event 댓글 이벤트 (댓글 ID, 게시글 ID, 작성자, 수신자 정보)
     */
    public void publishCommentEvent(CommentEvent event) {
        log.info("댓글 알림 이벤트 발행 - commentId: {}, postId: {}, receiver: {}",
                event.commentId(), event.postId(), event.receiverLoginId());

        // Spring Event 발행 - 동기적으로 발행하지만, 리스너가 @Async로 비동기 처리
        eventPublisher.publishEvent(event);

        log.debug("댓글 알림 이벤트 발행 완료");
    }

    /**
     * 좋아요 이벤트 발행
     * 게시글에 좋아요가 추가되면 이 메서드를 호출하여 알림 이벤트를 발행합니다.
     * 비동기 리스너가 이벤트를 수신하여 알림을 처리합니다.
     * @param event 좋아요 이벤트 (좋아요 ID, 게시글 ID, 수신자 정보)
     */
    public void publishLikeEvent(LikeEvent event) {
        log.info("좋아요 알림 이벤트 발행 - likeId: {}, postId: {}, receiver: {}",
                event.likeId(), event.postId(), event.receiverLoginId());

        // Spring Event 발행 - 동기적으로 발행하지만, 리스너가 @Async로 비동기 처리
        eventPublisher.publishEvent(event);

        log.debug("좋아요 알림 이벤트 발행 완료");
    }
}