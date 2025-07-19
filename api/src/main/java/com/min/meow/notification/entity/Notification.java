package com.min.meow.notification.entity;


import com.min.kafka.dto.NotificationDto;
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
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public static Notification toEntity(NotificationDto notificationDto){

        return Notification.builder()
                .commentId(notificationDto.getCommentId())
                .postId(notificationDto.getPostId())
                .message(notificationDto.getMessage())
                .receiverLoginId(notificationDto.getReceiverLoginId())
                .isRead(notificationDto.isRead())
                .build();
    }
}
