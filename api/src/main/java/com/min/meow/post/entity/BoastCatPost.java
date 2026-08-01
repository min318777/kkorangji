package com.min.meow.post.entity;

import com.min.meow.common.BasePost;
import com.min.meow.postlike.entity.PostLike;
import com.min.meow.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Check;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boast_cat_post", indexes = {
    // 목록 조회: ORDER BY created_at DESC + 페이징 (가장 빈번한 쿼리) , 커버링인덱스도 고려했지만 빈번하게 변하는 조회수, 댓글수, 좋아요수를 고려하여 단일인덱스적용
    @Index(name = "idx_boast_post_created_at", columnList = "created_at DESC"),
    // 마이페이지: WHERE user_id = ? ORDER BY created_at DESC
    @Index(name = "idx_boast_post_user_created_at", columnList = "user_id, created_at DESC")
})
@Check(constraints = "view >= 0 AND comment_count >= 0 AND like_count >= 0")
@SuperBuilder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Getter
public class BoastCatPost extends BasePost {

    /**
     * @BatchSize(100) 적용 이유:
     * - @ElementCollection은 1:N 관계와 동일하게 동작
     * - Fetch Join 시 카테시안 곱 발생 (게시글 데이터 중복)
     * - 목록 조회 시 N+1 문제 방지 (IN절로 배치 처리)
     * - 상세 조회 시에도 별도 쿼리 1개로 처리
     */
    // 목록 썸네일 URL (첫 번째 이미지, 없으면 null) — JOIN 없이 SELECT 가능
    @Column(length = 500)
    private String thumbnailUrl;

    @Builder.Default
    @BatchSize(size = 100)
    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "boastCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikeList = new ArrayList<>();

    @Builder.Default
    @ColumnDefault("0")
    private int likeCount = 0;     // 좋아요 수

    public void updatePost(String title, String contents, List<String> newImageUrls) {
        this.title = title;
        this.contents = contents;
        // 이미지는 keep/new/delete 조합으로 관리되므로 null이면 변경 없음
        if (newImageUrls != null) {
            this.imageUrls.clear();
            this.imageUrls.addAll(newImageUrls);
        }
    }
    // ID 기반 비교로 영속성 컨텍스트에 의존하지 않음
    public boolean isAuthor(User user) {
        return this.user.getId().equals(user.getId());
    }
}
