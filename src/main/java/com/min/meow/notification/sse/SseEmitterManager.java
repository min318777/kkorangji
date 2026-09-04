package com.min.meow.notification.sse;

import com.min.meow.notification.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 연결 관리자
 * - 한 사용자가 여러 탭을 열 경우 탭마다 SseEmitter를 별도로 관리
 * - CopyOnWriteArrayList: 읽기가 많고 쓰기가 적은 SSE 특성에 적합한 스레드 안전 리스트
 * - complete() 호출 금지 (연결 추가/제거 시)
 *   이유: Tomcat Async Dispatch가 Security Filter Chain을 재실행하면서
 *         새 스레드에 SecurityContext가 없어 AuthorizationDeniedException 발생
 *   해결: 리스트에서 remove()만 수행, 소켓은 heartbeat 실패 시 자연 정리
 */
@Slf4j
@Component
public class SseEmitterManager {

    // userId → 해당 사용자의 모든 탭 연결 목록
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private static final Long TIMEOUT = 30 * 60 * 1000L;

    /**
     * SSE 연결 생성 및 저장
     * 동일 사용자의 새 탭 연결은 리스트에 추가 (기존 연결 유지)
     */
    public SseEmitter createEmitter(Long userId, Long latestNotificationId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 사용자 리스트에 추가 (없으면 새 리스트 생성)
        CopyOnWriteArrayList<SseEmitter> userEmitters =
                emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);
        log.debug("SSE 연결 생성: userId={} (현재 {}개 탭)", userId, userEmitters.size());

        // 연결 종료 시 리스트에서 제거
        emitter.onCompletion(() -> removeOne(userId, emitter, "연결 종료"));
        emitter.onTimeout(() -> removeOne(userId, emitter, "타임아웃"));
        emitter.onError(e -> removeOne(userId, emitter, "에러"));

        // 초기 connect 이벤트 전송 (Nginx 504 방지, 재연결 주기 3초 지정)
        // latestNotificationId를 id로 포함 → 프론트 lastEventId 초기값으로 사용
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .reconnectTime(3000L)
                    .name("connect")
                    .data("connected");
            if (latestNotificationId != null) {
                event.id(String.valueOf(latestNotificationId));
            }
            emitter.send(event);
        } catch (IOException e) {
            removeOne(userId, emitter, "초기 이벤트 전송 실패");
            log.error("SSE 초기 이벤트 전송 실패: userId={}", userId, e);
        }

        return emitter;
    }

    /**
     * 특정 사용자의 모든 탭에 알림 전송
     */
    public void sendToUser(Long userId, NotificationResponse data) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            log.warn("SSE 연결 없는 사용자: userId={} (오프라인)", userId);
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(data.getId()))
                        .name("notification")
                        .data(data));
            } catch (IOException e) {
                removeOne(userId, emitter, "알림 전송 실패");
                emitter.completeWithError(e);
                log.error("알림 전송 실패: userId={}", userId, e);
            }
        }
        log.debug("알림 전송 완료: userId={} ({}개 탭)", userId, userEmitters.size());
    }

    /**
     * 로그아웃 시 해당 사용자의 모든 탭 연결 제거
     */
    public void removeEmitter(Long userId) {
        List<SseEmitter> userEmitters = emitters.remove(userId);
        if (userEmitters != null) {
            log.debug("로그아웃으로 SSE 연결 종료: userId={} ({}개 탭)", userId, userEmitters.size());
        }
    }

    public boolean isConnected(Long userId) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters != null && !userEmitters.isEmpty();
    }


    // 현재 연결된 전체 탭 수 조회
    public int getConnectedUserCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    // 30초마다 모든 연결에 ping → 실패 시 좀비 커넥션 즉시 제거
    @Scheduled(fixedDelay = 30000)
    public void heartbeat() {
        emitters.forEach((userId, userEmitters) ->
                userEmitters.forEach(emitter -> {
                    try {
                        emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                    } catch (IOException e) {
                        removeOne(userId, emitter, "heartbeat 실패");
                        emitter.completeWithError(e);
                        log.info("좀비 커넥션 제거: userId={}", userId);
                    }
                })
        );
    }

    // 리스트에서 특정 emitter 하나 제거, 리스트가 비면 Map 항목도 제거
    private void removeOne(Long userId, SseEmitter emitter, String reason) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;

        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId, userEmitters);
        }
        log.debug("SSE 연결 제거 [{}]: userId={}", reason, userId);
    }
}