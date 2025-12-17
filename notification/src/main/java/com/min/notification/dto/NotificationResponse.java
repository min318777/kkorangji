package com.min.notification.dto;

import com.min.meow.global.NotificationType;
import com.min.meow.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 조회 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private Long sourceId;          // 댓글 ID 또는 좋아요 ID
    private Long postId;             // 게시글 ID
    private NotificationType type;   // 알림 타입 (COMMENT, LIKE)
    private String message;          // 알림 메시지
    private String receiverLoginId;  // 받는 사람 로그인 ID
    private boolean isRead;          // 읽음 여부
    private LocalDateTime createdAt; // 생성 시간

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .sourceId(notification.getSourceId())
                .postId(notification.getPostId())
                .type(notification.getType())
                .message(notification.getMessage())
                .receiverLoginId(notification.getReceiverLoginId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
