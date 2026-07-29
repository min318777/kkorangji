package com.min.meow.notification.sse;

import com.min.meow.notification.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 연결 관리자
 * 역할: 사용자별로 SSE 연결을 저장하고 관리
 * - 사용자가 접속하면 연결을 저장
 * - 사용자에게 알림이 오면 저장된 연결로 실시간 전송
 * - ConcurrentHashMap: 멀티스레드 환경에서 안전하게 Map 사용
 * SSE 연결 교체 시 complete() 호출 금지!
 * 문제: 기존 연결에 complete()를 호출하면 Servlet Container(Tomcat)가
 *       Async Dispatch를 실행하여 Security Filter Chain을 재실행함.
 *       이때 새 스레드에는 SecurityContext가 없어서 AuthorizationDeniedException 발생.
 * 해결: put()으로 덮어쓰기 방식 사용. 기존 연결은 타임아웃/클라이언트 종료 시 자연스럽게 정리됨.
 */
@Slf4j
@Component
public class SseEmitterManager {

    // 사용자 userId(PK)를 Key로, SSE 연결을 Value로 저장
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 타임아웃: 30분 (밀리초)
    private static final Long TIMEOUT = 30 * 60 * 1000L;

    /**
     * SSE 연결 생성 및 저장
     * 중복 연결 처리 전략: put()으로 덮어쓰기
     * - 기존 연결에 complete() 호출 시 Servlet Async Dispatch가 발생하여
     *   Security Filter Chain이 재실행되고, 새 스레드에 SecurityContext가 없어서
     *   AuthorizationDeniedException이 발생하는 문제가 있었음.
     * - 해결: complete() 호출 없이 Map에서 덮어쓰기.
     * @param userId 사용자 ID (PK)
     * @return 생성된 SseEmitter
     */
    public SseEmitter createEmitter(Long userId) {
        // 1. 새로운 SSE 연결 생성 (타임아웃 30분)
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 2. Map에 저장 (기존 연결이 있으면 덮어쓰기 - complete() 호출 안 함!)
        SseEmitter oldEmitter = emitters.put(userId, emitter);
        if (oldEmitter != null) {
            log.debug("기존 SSE 연결 교체: userId={} (Map에서 제거됨, 소켓은 heartbeat가 정리)", userId);
        }
        log.debug("SSE 연결 생성: userId={} (현재 {}명 접속)", userId, emitters.size());

        // 3. 연결 종료 시 Map에서 제거 (현재 emitter일 때만)
        emitter.onCompletion(() -> {
            boolean removed = emitters.remove(userId, emitter);
            if (removed) {
                log.debug("SSE 연결 종료: userId={}", userId);
            }
        });

        // 4. 타임아웃 시 Map에서 제거 (현재 emitter일 때만)
        emitter.onTimeout(() -> {
            boolean removed = emitters.remove(userId, emitter);
            if (removed) {
                log.debug("SSE 타임아웃: userId={} - 클라이언트 재연결 필요", userId);
            }
        });

        // 5. 에러 발생 시 Map에서 제거 (현재 emitter일 때만)
        emitter.onError(e -> {
            boolean removed = emitters.remove(userId, emitter);
            if (removed) {
                log.error("SSE 에러: userId={}", userId, e);
            }
        });

        // 6. 초기 연결 확인 이벤트 전송 (Nginx 504 타임아웃 방지)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
            log.error("SSE 초기 이벤트 전송 실패: userId={}", userId, e);
        }

        return emitter;
    }

    /**
     * 특정 사용자에게 알림 전송
     * @param userId 받을 사람 사용자 ID (PK)
     * @param data 전송할 데이터
     */
    public void sendToUser(Long userId, NotificationResponse data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.warn("SSE 연결이 없는 사용자: userId={} (사용자가 오프라인 상태)", userId);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")  // 이벤트 이름
                    .data(data));          // 전송할 데이터
            log.debug("알림 전송 성공: userId={}", userId);
        } catch (IOException e) {
            // remove(userId, emitter): 이 emitter일 때만 제거 (새로 교체된 emitter 보호)
            emitters.remove(userId, emitter);
            emitter.completeWithError(e);
            log.error("알림 전송 실패: userId={}", userId, e);
        }
    }

    /**
     * 로그아웃 시 SSE 연결 종료
     */
    public void removeEmitter(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.debug("로그아웃으로 SSE 연결 종료: userId={}", userId);
        }
    }

    /**
     * 사용자가 연결되어 있는지 확인
     * @param userId 확인할 사용자 ID (PK)
     * @return 연결 여부
     */
    public boolean isConnected(Long userId) {
        return emitters.containsKey(userId);
    }

    /**
     * 현재 연결된 사용자 수 조회
     * @return 연결된 사용자 수
     */
    public int getConnectedUserCount() {return emitters.size();
    }

    // 30초마다 모든 연결에 ping 전송 → 실패 시 좀비 커넥션 즉시 제거
    @Scheduled(fixedDelay = 30000)
    public void heartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (IOException e) {
                emitters.remove(userId, emitter);
                emitter.completeWithError(e);  // Tomcat 서블릿 리소스 정리
                log.info("좀비 커넥션 제거: userId={}", userId);
            }
        });
    }
}
