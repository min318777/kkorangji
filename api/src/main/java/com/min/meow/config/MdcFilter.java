package com.min.meow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC(Mapped Diagnostic Context) 필터
 * 모든 HTTP 요청에 고유한 requestId를 부여하여 로그 추적을 가능하게 합니다.
 *
 * 왜 MDC를 사용하는가?
 * - 동시 요청이 많을 때 로그가 뒤섞여 특정 요청의 흐름 추적이 불가능해짐
 * - MDC는 스레드 로컬 기반으로 요청별 컨텍스트 정보를 로그에 자동 포함시킴
 * - k6 부하 테스트(1,000 VU) 환경에서도 특정 요청 추적 가능
 */
@Component
public class MdcFilter extends OncePerRequestFilter {

    // MDC 키 상수: logback-spring.xml 패턴의 %X{requestId}와 일치해야 함
    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 요청마다 고유한 UUID를 생성하여 MDC에 등록
        // UUID v4: 무작위 128비트 값, 충돌 확률 극히 낮음
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_KEY, requestId);

        // 응답 헤더에도 requestId를 포함시켜 클라이언트 측 디버깅 지원
        response.setHeader("X-Request-Id", requestId);

        try {
            // 다음 필터로 요청 전달 (JwtAuthenticationFilter → Controller → ...)
            filterChain.doFilter(request, response);
        } finally {
            // 요청 완료 후 반드시 MDC를 초기화해야 함
            // ThreadPool 환경에서 스레드가 재사용될 때 이전 요청의 컨텍스트가 남아있지 않도록 방지
            MDC.clear();
        }
    }
}
