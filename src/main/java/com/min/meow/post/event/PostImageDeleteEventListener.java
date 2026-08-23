package com.min.meow.post.event;

import com.min.meow.config.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트랜잭션 커밋 후(AFTER_COMMIT) 비동기로 S3 이미지를 삭제한다.
 * DB 삭제/수정 트랜잭션이 S3 API 응답을 기다리지 않도록 분리 — 실패해도 DB 정합성에는 영향 없음(고아 파일만 남음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostImageDeleteEventListener {

    private final S3Service s3Service;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostImageDeleteEvent event) {
        try {
            s3Service.deleteFiles(event.imageKeys());
        } catch (Exception e) {
            log.error("S3 이미지 삭제 실패, 고아 파일 발생 가능 - keys: {}", event.imageKeys(), e);
        }
    }
}
