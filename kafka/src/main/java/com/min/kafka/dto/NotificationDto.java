package com.min.kafka.dto;


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

    /*
    public static NotificationDto toDto(Notice notice){

        return NotificationDto.builder()
                .id(notice.getNoticeId())
                .message(notice.getMessage())
                .postId(notice.getPostId())
                .receiverLoginId(notice.getReceiverLoginId())
                .isRead(notice.isRead())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
     */
}