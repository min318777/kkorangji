package com.min.meow.notification.controller;

import com.min.meow.common.ApiResponse;
import com.min.meow.common.PageResponse;
import com.min.meow.notification.dto.request.NotificationRequest;
import com.min.meow.notification.dto.response.NotificationResponse;
import com.min.meow.notification.service.NotificationQueryService;
import com.min.meow.notification.sse.SseEmitterManager;
import com.min.meow.common.PrincipalUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "알림", description = "알림 조회,읽음 처리 및 SSE 구독 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final SseEmitterManager sseEmitterManager;

    @Operation(summary = "알림 목록 조회",
            description = "로그인한 사용자의 알림 목록을 페이징 조회합니다. 인증 필요.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getAllNotifications(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "5")
            @RequestParam(defaultValue = "5") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        Long userId = user.getUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationQueryService.getAllNotifications(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("알림 목록 조회 성공", PageResponse.from(notifications)));
    }

    @Operation(summary = "단일 알림 읽음 처리",
            description = "특정 알림을 읽음 상태로 변경합니다. 본인 알림만 처리 가능합니다. 인증 필요.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> readSingleNotification(
            @Parameter(description = "알림 ID", example = "1")
            @PathVariable Long notificationId,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        Long userId = user.getUserId();
        log.debug("단일 알림 읽음 요청 - NotificationId: {}, UserId: {}", notificationId, userId);
        NotificationResponse notificationResponse = notificationQueryService
                .readSingleNotification(notificationId, userId);

        return ResponseEntity.ok(ApiResponse.success("알림 읽음 처리 완료", notificationResponse));
    }

    @Operation(summary = "다건 알림 읽음 처리",
            description = "여러 알림을 일괄 읽음 처리합니다. 존재하지 않거나 권한 없는 알림은 자동 필터링됩니다. 인증 필요.")
    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<ReadCountResponse>> readMultipleNotifications(
            @Valid @RequestBody NotificationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        Long userId = user.getUserId();
        log.debug("여러 개 알림 읽음 요청 - Count: {}, UserId: {}",
                request.getNotificationIds().size(), userId);
        int readCount = notificationQueryService
                .readMultipleNotifications(request.getNotificationIds(), userId);

        ReadCountResponse data = new ReadCountResponse(readCount, request.getNotificationIds().size());
        return ResponseEntity.ok(ApiResponse.success(
                readCount + "개의 알림을 읽음 처리했습니다.", data));
    }

    @Operation(summary = "전체 알림 읽음 처리",
            description = "현재 사용자의 읽지 않은 모든 알림을 일괄 읽음 처리합니다. 인증 필요.")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<ReadCountResponse>> readAllNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user) {

        Long userId = user.getUserId();
        log.debug("전체 알림 읽음 요청 - UserId: {}", userId);

        int readCount = notificationQueryService.readAllNotifications(userId);

        ReadCountResponse data = new ReadCountResponse(readCount, readCount);
        return ResponseEntity.ok(ApiResponse.success("모든 알림을 읽음 처리했습니다.", data));
    }

    @Operation(summary = "SSE 구독",
            description = "실시간 알림 수신을 위한 SSE 연결을 생성합니다. text/event-stream 형식으로 응답합니다. 인증 필요.")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(hidden = true) @AuthenticationPrincipal PrincipalUser user,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            jakarta.servlet.http.HttpServletResponse response) {

        // Nginx 계열 리버스 프록시의 버퍼링을 응답 헤더로 직접 비활성화
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        Long userId = user.getUserId();
        log.debug("SSE 구독 요청 - UserId: {}, lastEventId: {}", userId, lastEventId);

        // connect 이벤트 id로 사용할 최신 알림 ID 조회 (프론트 lastEventId 초기값)
        Long latestNotificationId = notificationQueryService.getLatestNotificationId(userId);
        SseEmitter emitter = sseEmitterManager.createEmitter(userId, latestNotificationId);

        // 재연결 시 끊긴 동안 누락된 알림 재전송
        if (lastEventId != null && !lastEventId.isBlank()) {
            notificationQueryService
                    .getMissedNotifications(userId, Long.parseLong(lastEventId))
                    .forEach(n -> sseEmitterManager.sendToUser(userId, n));
        }

        return emitter;
    }

    @Operation(summary = "연결 상태 조회",
            description = "현재 SSE로 연결된 사용자 수를 조회합니다. 디버깅 용도. 인증 불필요.")
    @SecurityRequirements
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> getStatus() {
        int count = sseEmitterManager.getConnectedUserCount();
        String statusMessage = "현재 연결된 사용자: " + count + "명";
        return ResponseEntity.ok(ApiResponse.success("연결 상태 조회 성공", statusMessage));
    }

    /**
     * 읽음 처리 결과 응답 DTO
     */
    public record ReadCountResponse(int readCount, int requestedCount) {}
}
