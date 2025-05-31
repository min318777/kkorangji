package com.min.meow.notice.entity;


import com.min.kafka.dto.NotificationDto;
import com.min.meow.notice.NoticeDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;

    private Long commentId;

    private Long postId;

    private String message;

    private String receiverLoginId;

    private boolean isRead;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    public static Notice toEntity(NotificationDto notificationDto){

        return Notice.builder()
                .commentId(notificationDto.getCommentId())
                .postId(notificationDto.getPostId())
                .message(notificationDto.getMessage())
                .receiverLoginId(notificationDto.getReceiverLoginId())
                .isRead(notificationDto.isRead())
                .build();
    }
}
