package com.min.meow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.min.meow.user.service.DauService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC(Mapped Diagnostic Context) 필터
 * 모든 HTTP 요청에 고유한 requestId를 부여하여 로그 추적을 가능하게 합니다.
 * 왜 MDC를 사용하는가?
 * - 동시 요청이 많을 때 로그가 뒤섞여 특정 요청의 흐름 추적이 불가능해짐
 * - MDC는 스레드 로컬 기반으로 요청별 컨텍스트 정보를 로그에 자동 포함시킴
 * - k6 부하 테스트(1,000 VU) 환경에서도 특정 요청 추적 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MdcFilter extends OncePerRequestFilter {

    private final DauService dauService;

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String CLIENT_IP_KEY = "clientIp";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_KEY, requestId);

        // nginx 뒤에서는 X-Forwarded-For 헤더에 실제 클라이언트 IP가 담김
        // 여러 프록시 경유 시 "client, proxy1, proxy2" 형태 → 첫 번째가 실제 IP
        String clientIp = extractClientIp(request);
        MDC.put(CLIENT_IP_KEY, clientIp);
        dauService.recordByIp(clientIp);

        // 응답 헤더에도 requestId를 포함시켜 클라이언트 측 디버깅 지원
        response.setHeader("X-Request-Id", requestId);

        long start = System.currentTimeMillis();
        try {
            // 다음 필터로 요청 전달 (JwtAuthenticationFilter → Controller → ...)
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            // 5xx 원인은 GlobalExceptionHandler가 스택트레이스와 함께 error로 남기고 Sentry로 전송함.
            // 여기서 또 error를 찍으면 같은 요청에 대해 Sentry 이벤트가 중복 생성되므로 접근 로그 수준(info)으로만 남긴다.
            log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
            MDC.clear();
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].strip();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
