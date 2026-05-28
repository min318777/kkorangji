package com.min.meow.comment.entity;

import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment", indexes = {
    // 게시글별 원댓글 조회: WHERE boast_cat_post_id = ? ORDER BY created_at DESC
    @Index(name = "idx_comment_boast_post_created_at", columnList = "boast_cat_post_id, created_at DESC"),
    // 게시글별 원댓글 조회: WHERE lost_cat_post_id = ? ORDER BY created_at DESC
    @Index(name = "idx_comment_lost_post_created_at", columnList = "lost_cat_post_id, created_at DESC"),
    // 마이페이지 내 댓글: WHERE user_id = ? ORDER BY created_at DESC
    @Index(name = "idx_comment_user_created_at", columnList = "user_id, created_at DESC"),
    // 대댓글 조회: WHERE parent_comment_id = ? ORDER BY created_at ASC
    @Index(name = "idx_comment_parent_created_at", columnList = "parent_comment_id, created_at ASC")
})
@Getter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String contents;

    // 소프트 삭제: 대댓글이 있는 원댓글 삭제 시 "삭제된 댓글입니다"로 표시
    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false;

    // 자기 참조: null = 원댓글, 값 있으면 대댓글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id",
        foreignKey = @ForeignKey(name = "fk_comment_parent"))
    private Comment parentComment;

    // FK 삭제 전략: 사용자 삭제 시 댓글이 존재하면 삭제 차단
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_comment_user",
            foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE RESTRICT"))
    private User user;

    // FK 삭제 전략: 실종 게시글 삭제 시 관련 댓글도 함께 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_cat_post_id",
        foreignKey = @ForeignKey(name = "fk_comment_lost_cat_post",
            foreignKeyDefinition = "FOREIGN KEY (lost_cat_post_id) REFERENCES lost_cat_post(id) ON DELETE CASCADE"))
    private LostCatPost lostCatPost;

    // FK 삭제 전략: 자랑 게시글 삭제 시 관련 댓글도 함께 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boast_cat_post_id",
        foreignKey = @ForeignKey(name = "fk_comment_boast_cat_post",
            foreignKeyDefinition = "FOREIGN KEY (boast_cat_post_id) REFERENCES boast_cat_post(id) ON DELETE CASCADE"))
    private BoastCatPost boastCatPost;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 댓글 작성자 확인 (ID 기반 비교로 영속성 컨텍스트에 의존하지 않음)
    public boolean isAuthor(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void updateContent(String content) {
        if (content != null) {
            this.contents = content;
        }
    }

    // 소프트 삭제 처리
    public void softDelete() {
        this.isDeleted = true;
        this.contents = "삭제된 댓글입니다.";
    }

}