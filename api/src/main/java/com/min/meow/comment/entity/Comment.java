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
    // 게시글별 댓글 조회: WHERE boast_cat_post_id = ? ORDER BY created_at DESC
    @Index(name = "idx_comment_boast_post_created_at", columnList = "boast_cat_post_id, created_at DESC"),
    // 게시글별 댓글 조회: WHERE lost_cat_post_id = ? ORDER BY created_at DESC
    @Index(name = "idx_comment_lost_post_created_at", columnList = "lost_cat_post_id, created_at DESC"),
    // 마이페이지 내 댓글: WHERE user_id = ? ORDER BY created_at DESC
    @Index(name = "idx_comment_user_created_at", columnList = "user_id, created_at DESC")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DB 레벨 제약 (검증은 Request DTO에서 처리)
    @Column(nullable = false, length = 500)
    private String contents;

    private boolean isRead;

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

    // 댓글 내용 수정
    public void updateContent(String content) {
        if (content != null) {
            this.contents = content;
        }
    }

    // 읽음 상태로 변경
    public void markAsRead() {
        this.isRead = true;
    }
}