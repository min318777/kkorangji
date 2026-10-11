package com.min.meow.common;

import com.min.meow.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ColumnDefault("0")
    protected int view;

    @Builder.Default
    @ColumnDefault("0")
    protected int commentCount = 0;

    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    // 목록 썸네일 URL (첫 번째 이미지, 없으면 null)
    @Column(length = 500)
    protected String thumbnailUrl;

    @Builder.Default
    @ElementCollection
    protected List<String> imageUrls = new ArrayList<>();

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

    public boolean isAuthor(Long userId) {
        return this.user.getId().equals(userId);
    }

    public boolean canNotify(Long actorUserId) {
        return !this.user.isWithdrawn() && !this.user.getId().equals(actorUserId);
    }

    // 이미지는 keep/new/delete 조합으로 관리되므로 null이면 변경 없음
    protected void replaceImages(List<String> newImageUrls) {
        if (newImageUrls == null) return;
        this.imageUrls.clear();
        this.imageUrls.addAll(newImageUrls);
        this.thumbnailUrl = newImageUrls.isEmpty() ? null : newImageUrls.get(0);
    }
}
