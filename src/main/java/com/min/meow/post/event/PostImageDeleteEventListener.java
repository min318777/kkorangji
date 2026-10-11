package com.min.meow.post.event;

import com.min.meow.config.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 트랜잭션 커밋 후(AFTER_COMMIT) 비동기로 S3 이미지 삭제 — DB 트랜잭션이 S3 응답을 안 기다리도록 분리(실패해도 고아 파일만 남고 DB 정합성엔 무영향)
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
