package com.min.meow.post.entity;

import com.min.meow.global.BasePost;
import com.min.meow.comment.entity.Comment;
import com.min.meow.postlike.entity.PostLike;
import com.min.meow.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BoastCatPost extends BasePost {

    /**
     * @BatchSize(100) 적용 이유:
     * - @ElementCollection은 1:N 관계와 동일하게 동작
     * - Fetch Join 시 카테시안 곱 발생 (게시글 데이터 중복)
     * - 목록 조회 시 N+1 문제 방지 (IN절로 배치 처리)
     * - 상세 조회 시에도 별도 쿼리 1개로 처리
     */
    @BatchSize(size = 100)
    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    @OneToMany(mappedBy = "boastCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikeList = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "boastCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // 비정규화 필드: 조회 성능 최적화를 위해 카운트를 직접 저장
    @Builder.Default
    private int commentCount = 0;  // 댓글 수

    @Builder.Default
    private int likeCount = 0;     // 좋아요 수

    // 댓글 수 증가
    public void incrementCommentCount() {
        this.commentCount++;
    }

    // 댓글 수 감소
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    // 좋아요 수 증가
    public void incrementLikeCount() {
        this.likeCount++;
    }

    // 좋아요 수 감소
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void updatePost(String title, String contents, List<String> newImageUrls) {
        if (title != null) {
            this.title = title;
        }
        if (contents != null) {
            this.contents = contents;
        }
        if (newImageUrls != null) {
            this.imageUrls.clear();
            this.imageUrls.addAll(newImageUrls);
        }
    }
    public boolean isAuthor(User user) {
        return this.user.equals(user);
    }
}
