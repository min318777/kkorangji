package com.min.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS(Cross-Origin Resource Sharing) 설정
 *
 * - 프론트엔드(localhost:3000)에서 notification 서버(8082)로의 요청 허용
 * - SSE 연결을 위해 필수적인 설정
 */
@Configuration
public class CorsMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 모든 /api/** 엔드포인트에 CORS 허용
        registry.addMapping("/api/**")
                .exposedHeaders("Set-Cookie")
                .allowedOrigins("http://localhost:3000")  // 프론트엔드 주소
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);  // 쿠키 및 인증 정보 허용
    }
}
