package com.min.meow.notice;

import com.min.kafka.dto.NotificationDto;
import com.min.meow.notice.entity.Notice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeDto {

    private Long commentId;
    private Long postId;
    private String message;
    private String receiverLoginId;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NoticeDto toDto(Notice notice){

        return NoticeDto.builder()
                .commentId(notice.getId())
                .message(notice.getMessage())
                .postId(notice.getPostId())
                .receiverLoginId(notice.getReceiverLoginId())
                .isRead(notice.isRead())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
    public static NotificationDto toKafkaDto(NoticeDto noticeDto){

        return NotificationDto.builder()
                .commentId(noticeDto.getCommentId())
                .postId(noticeDto.getPostId())
                .message(noticeDto.getMessage())
                .receiverLoginId(noticeDto.getReceiverLoginId())
                .isRead(noticeDto.isRead)
                .build();
    }
}
