package com.min.meow.notification.domain;

import com.min.meow.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationDto {

    private Long commentId;
    private Long postId;
    private String message;
    private String receiverLoginId;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationDto toDto(Notification notification){

        return NotificationDto.builder()
                .commentId(notification.getId())
                .message(notification.getMessage())
                .postId(notification.getPostId())
                .receiverLoginId(notification.getReceiverLoginId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
    public static com.min.kafka.dto.NotificationDto toKafkaDto(NotificationDto notificationDto){

        return com.min.kafka.dto.NotificationDto.builder()
                .commentId(notificationDto.getCommentId())
                .postId(notificationDto.getPostId())
                .message(notificationDto.getMessage())
                .receiverLoginId(notificationDto.getReceiverLoginId())
                .isRead(notificationDto.isRead())
                .createdAt(notificationDto.getCreatedAt())
                .build();
    }
}
