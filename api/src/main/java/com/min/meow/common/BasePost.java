package com.min.meow.common;

import com.min.meow.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@SuperBuilder
public abstract class BasePost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    // DB 레벨 제약 (검증은 Request DTO에서 처리)
    @Column(nullable = false, length = 100)
    protected String title;

    @Column(nullable = false, length = 2000)
    protected String contents;

    // FK 삭제 전략: 사용자 삭제 시 게시글이 존재하면 삭제 차단 (DB 마지막 방어선)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_post_user",
            foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE RESTRICT"))
    protected User user;

    // DB 레벨 제약: 조회수 기본값 0
    @ColumnDefault("0")
    protected int view;

    // 비정규화 필드: 댓글 수 직접 저장 (JOIN 없이 조회 가능)
    @Builder.Default
    @ColumnDefault("0")
    protected int commentCount = 0;

    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementView() {
        this.view++;
    }
}
