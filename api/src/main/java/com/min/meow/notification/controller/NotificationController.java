package com.min.meow.notification.controller;


import com.min.meow.config.PrincipalUser;
import com.min.meow.global.PageResponse;
import com.min.meow.global.exception.ApiResponse;
import com.min.meow.global.exception.ErrorResponse;
import com.min.meow.notification.dto.NotificationResponse;
import com.min.meow.notification.service.NotificationService;
import com.min.meow.notification.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/notice")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterManager sseEmitterManager;

    @GetMapping
    public ResponseEntity<?> getAllNotice(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam (defaultValue = "5") int size){

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notices = notificationService.getAllNotifications(pageable);
        PageResponse<NotificationResponse> pageResponse = PageResponse.from(notices);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "댓글 알림 조회 성공", pageResponse));
    }

    /**
     * 📌 투두: 2단계 - SSE 구독 API
     * 역할: 프론트엔드가 이 API를 호출하면 실시간 알림 연결 생성
     * 왜 필요한가?
     * - 프론트엔드: "알림 받을게요!" 하고 연결
     * - 백엔드: 연결을 저장하고 알림 올 때마다 전송
     * 흐름:
     * 1. 사용자가 로그인한 상태에서 이 API 호출
     * 2. SseEmitterManager에 연결 저장
     * 3. 연결 성공 메시지 전송
     * 4. 이후 알림 발생 시 자동으로 전송됨
     * */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal PrincipalUser principalUser) {
        // 1. 로그인한 사용자 ID 가져오기
        String userLoginId = principalUser.getUsername();
        log.info("📞 SSE 구독 요청: {}", userLoginId);

        // 2. SSE 연결 생성 및 저장
        SseEmitter emitter = sseEmitterManager.createEmitter(userLoginId);

        // 3. 연결 성공 메시지 전송 (프론트엔드가 연결 확인용)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")  // 이벤트 이름: connect
                    .data("SSE 연결 성공!"));

            log.info("✅ SSE 연결 성공: {}", userLoginId);
        } catch (Exception e) {
            log.error("❌ SSE 초기 메시지 전송 실패: {}", userLoginId);
            throw new RuntimeException("SSE 연결 실패");
        }

        return emitter;
    }
}
