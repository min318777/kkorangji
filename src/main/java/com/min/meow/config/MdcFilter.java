package com.min.meow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.min.meow.user.service.DauService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// 모든 HTTP 요청에 고유 requestId를 부여해 동시 요청 환경에서도 로그 추적이 가능하게 하는 MDC 필터
@Slf4j
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

    // Security 필터 체인(order=-100)보다 먼저 실행되어야 로그인 실패 로그에도 requestId가 남으므로 최우선 순위로 등록
    @Configuration
    @RequiredArgsConstructor
    static class MdcFilterConfig {

        private final DauService dauService;

        @Bean
        public FilterRegistrationBean<MdcFilter> mdcFilterRegistration() {
            FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>(new MdcFilter(dauService));
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return registration;
        }
    }
}
