package com.min.meow.notification.dto;


import com.min.meow.global.NotificationType;
import com.min.meow.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private Long sourceId;
    private Long postId;
    private NotificationType type;
    private String message;
    private String receiverLoginId;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


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
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
