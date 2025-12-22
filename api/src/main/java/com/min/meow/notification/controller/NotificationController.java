package com.min.meow.notification.controller;

import com.min.meow.notification.dto.request.NotificationRequest;
import com.min.meow.notification.dto.response.NotificationResponse;
import com.min.meow.notification.service.NotificationQueryService;
import com.min.meow.notification.sse.SseEmitterManager;
import com.min.meow.user.dto.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

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
     * 단일 알림 읽음 처리
     * - RESTful한 설계: 특정 리소스({id})에 대한 상태 변경
     * @param notificationId 읽음 처리할 알림 ID
     * @param customUserDetails 인증된 사용자 정보 (JWT에서 자동 추출)
     * @return 읽음 처리된 알림 정보
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> readSingleNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        String userLoginId = customUserDetails.getUser().getLoginId();
        log.info("단일 알림 읽음 요청 - NotificationId: {}, User: {}", notificationId, userLoginId);
        NotificationResponse response = notificationQueryService
                .readSingleNotification(notificationId, userLoginId);

        return ResponseEntity.ok(response);
    }

    /**
     * 여러 개의 알림 읽음 처리
     * - Request Body에 알림 ID 목록을 받아서 일괄 처리
     * - 존재하지 않는 ID나 권한 없는 알림은 자동으로 필터링됨
     * @param request 읽음 처리할 알림 ID 목록
     * @param customUserDetails 인증된 사용자 정보 (JWT에서 자동 추출)
     * @return 읽음 처리된 알림 개수
     */
    @PatchMapping("/read")
    public ResponseEntity<Map<String, Object>> readMultipleNotifications(
            @Valid @RequestBody NotificationRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        String userLoginId = customUserDetails.getUser().getLoginId();
        log.info("여러 개 알림 읽음 요청 - Count: {}, User: {}",
                request.getNotificationIds().size(), userLoginId);
        int readCount = notificationQueryService
                .readMultipleNotifications(request.getNotificationIds(), userLoginId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", readCount + "개의 알림을 읽음 처리했습니다.");
        response.put("readCount", readCount);
        response.put("requestedCount", request.getNotificationIds().size());

        return ResponseEntity.ok(response);
    }

    /**
     * 전체 알림 읽음 처리
     * - 요청한 사용자의 모든 읽지 않은 알림을 일괄 읽음 처리
     * - "모두 읽음" 기능 구현에 사용
     * @param customUserDetails 인증된 사용자 정보 (JWT에서 자동 추출)
     * @return 읽음 처리된 알림 개수
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> readAllNotifications(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        String userLoginId = customUserDetails.getUser().getLoginId();
        log.info("전체 알림 읽음 요청 - User: {}", userLoginId);

        int readCount = notificationQueryService.readAllNotifications(userLoginId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "모든 알림을 읽음 처리했습니다.");
        response.put("readCount", readCount);

        return ResponseEntity.ok(response);
    }
    /**
     * SSE 구독 엔드포인트
     * - 프론트엔드가 실시간 알림을 받기 위해 연결
     * - JWT 토큰으로 사용자 인증
     * @param customUserDetails 인증된 사용자 정보 (JWT에서 자동 추출)
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        String userLoginId = customUserDetails.getUser().getLoginId();
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
