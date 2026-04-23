package com.min.meow.notification.entity;


import com.min.meow.global.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification", indexes = {
    // 미읽음 알림 조회: WHERE receiver_user_id = ? AND is_read = false
    @Index(name = "idx_notification_receiver_read", columnList = "receiver_user_id, is_read"),
    // 전체 알림 목록: ORDER BY created_at DESC
    @Index(name = "idx_notification_created_at", columnList = "created_at DESC")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DB 레벨 제약: 알림 출처 (댓글 ID, 좋아요 ID 등)
    private Long sourceId;

    // DB 레벨 제약: 관련 게시글은 반드시 존재해야 함
    @Column(nullable = false)
    private Long postId;

    // DB 레벨 제약: 알림 타입은 반드시 존재해야 함
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // DB 레벨 제약: 알림 메시지는 반드시 존재해야 함
    @Column(nullable = false, length = 500)
    private String message;

    // DB 레벨 제약: 수신자 userId(PK)는 반드시 존재해야 함
    @Column(nullable = false)
    private Long receiverUserId;

    private boolean isRead;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.isRead = true;
    }

}
