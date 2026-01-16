package com.min.meow.notification.sse;

import lombok.extern.slf4j.Slf4j;
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
 */
@Slf4j
@Component
public class SseEmitterManager {

    // 사용자 로그인ID를 Key로, SSE 연결을 Value로 저장
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 타임아웃: 30분 (밀리초)
    private static final Long TIMEOUT = 30 * 60 * 1000L;

    /**
     * SSE 연결 생성 및 저장
     * @param userLoginId 사용자 로그인 ID
     * @return 생성된 SseEmitter
     */
    public SseEmitter createEmitter(String userLoginId) {
        // 1. 새로운 SSE 연결 생성 (타임아웃 30분)
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 2. 기존 연결이 있으면 종료 (중복 방지)
        if (emitters.containsKey(userLoginId)) {
            emitters.get(userLoginId).complete();
            log.info("연결 종료: {}", userLoginId);
        }

        // 3. Map에 저장
        emitters.put(userLoginId, emitter);
        log.info("SSE 연결 생성: {} (현재 {}명 접속)", userLoginId, emitters.size());

        // 4. 연결이 완료되면 Map에서 제거
        emitter.onCompletion(() -> {
            emitters.remove(userLoginId);
            log.info("SSE 연결 완료: {}", userLoginId);
        });

        // 5. 타임아웃 시 Map에서 제거하고 연결 완료 처리
        emitter.onTimeout(() -> {
            emitters.remove(userLoginId);
            emitter.complete();  // 타임아웃 시 명시적으로 완료 처리
            log.debug("SSE 타임아웃: {} - 클라이언트 재연결 필요", userLoginId);
        });

        // 6. 에러 발생 시 Map에서 제거
        emitter.onError(e -> {
            emitters.remove(userLoginId);
            log.error("SSE 에러: {}", userLoginId, e);
        });

        return emitter;
    }

    /**
     * 특정 사용자에게 알림 전송
     * @param userId 받을 사람 로그인 ID
     * @param data 전송할 데이터
     */
    public void sendToUser(String userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.warn("SSE 연결이 없는 사용자: {} (사용자가 오프라인 상태)", userId);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")  // 이벤트 이름
                    .data(data));          // 전송할 데이터
            log.info("알림 전송 성공: {}", userId);
        } catch (IOException e) {
            emitters.remove(userId);
            emitter.completeWithError(e);
            log.error("알림 전송 실패: {}", userId, e);
        }
    }

    /**
     * 사용자가 연결되어 있는지 확인
     * @param userId 확인할 사용자 로그인 ID
     * @return 연결 여부
     */
    public boolean isConnected(String userId) {
        return emitters.containsKey(userId);
    }

    /**
     * 현재 연결된 사용자 수 조회
     * @return 연결된 사용자 수
     */
    public int getConnectedUserCount() {
        return emitters.size();
    }
}
