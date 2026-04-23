package com.min.meow.notification.dto.response;

import com.min.meow.global.NotificationType;
import com.min.meow.notification.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 조회 응답 DTO
 */
@Schema(description = "알림 조회 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    @Schema(description = "알림 ID", example = "1")
    private Long id;

    @Schema(description = "알림 발생 소스 ID (댓글 ID 또는 좋아요 ID)", example = "10")
    private Long sourceId;

    @Schema(description = "관련 게시글 ID", example = "5")
    private Long postId;

    @Schema(description = "알림 타입", example = "COMMENT_ADDED")
    private NotificationType type;

    @Schema(description = "알림 메시지", example = "cat_lover님이 댓글을 달았습니다.")
    private String message;

    @Schema(description = "알림 수신자 사용자 ID", example = "1")
    private Long receiverUserId;

    @Schema(description = "읽음 여부", example = "false")
    private boolean isRead;

    @Schema(description = "알림 생성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .sourceId(notification.getSourceId())
                .postId(notification.getPostId())
                .type(notification.getType())
                .message(notification.getMessage())
                .receiverUserId(notification.getReceiverUserId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
