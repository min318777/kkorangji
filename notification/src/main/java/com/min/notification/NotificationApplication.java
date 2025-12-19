package com.min.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Notification Service - 알림 전용 마이크로서비스
 * - Kafka에서 알림 이벤트 수신 (Consumer)
 * - 알림 데이터베이스 저장
 * - SSE(Server-Sent Events)를 통한 실시간 알림 전송
 * - 알림 조회 API 제공
 */
@SpringBootApplication(scanBasePackages = {
        "com.min.notification",
        "com.min.meow.notification"
})
@EnableJpaRepositories(basePackages = "com.min.meow.notification.repository")
@EntityScan(basePackages = "com.min.meow.notification.entity")
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
