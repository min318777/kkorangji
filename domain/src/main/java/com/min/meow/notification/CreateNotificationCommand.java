package com.min.meow.notification;

import com.min.meow.global.NotificationType;

/**
 * 알림 생성 커맨드
 * 왜 필요한가?
 * - Kafka Consumer에서 알림을 생성할 때 필요한 모든 정보를 담음
 */
public record CreateNotificationCommand (
        Long sourceId,
        Long postId,
        String receiverLoginId,
        NotificationType type,
        String message
){}

