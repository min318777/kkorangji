package com.min.notification.controller;

import com.min.notification.dto.NotificationResponse;
import com.min.notification.service.NotificationQueryService;
import com.min.notification.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Notification API Controller
 * - SSE 구독 엔드포인트
 * - 알림 조회 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final SseEmitterManager sseEmitterManager;

    /**
     * 알림 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationQueryService.getAllNotifications(pageable);

        return ResponseEntity.ok(notifications);
    }

    /**
     * SSE 구독 엔드포인트
     * - 프론트엔드가 실시간 알림을 받기 위해 연결
     * - Header에서 사용자 ID를 받음 (추후 JWT 토큰으로 대체 가능)
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestHeader("X-User-Login-Id") String userLoginId) {
        log.info("구독 요청 - User: {}", userLoginId);

        // SSE 연결 생성 및 저장
        SseEmitter emitter = sseEmitterManager.createEmitter(userLoginId);

        // 연결 성공 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결 성공!"));

            log.info("연결 성공 - User: {}", userLoginId);
        } catch (Exception e) {
            log.error("초기 메시지 전송 실패 - User: {}", userLoginId, e);
            throw new RuntimeException("SSE 연결 실패");
        }

        return emitter;
    }

    /**
     * 현재 연결된 사용자 수 조회 (디버깅용)
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        int count = sseEmitterManager.getConnectedUserCount();
        return ResponseEntity.ok("현재 연결된 사용자: " + count + "명");
    }
}
