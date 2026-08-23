package com.min.meow.post.entity;

import com.min.meow.common.BasePost;
import com.min.meow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
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

    // 목록 썸네일 URL (첫 번째 이미지, 없으면 null) — JOIN 없이 SELECT 가능
    @Column(length = 500)
    private String thumbnailUrl;

    @Builder.Default
    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    @Builder.Default
    @ColumnDefault("0")
    private int likeCount = 0;

    public void updatePost(String title, String contents, List<String> newImageUrls) {
        this.title = title;
        this.contents = contents;
        if (newImageUrls != null) {
            this.imageUrls.clear();
            this.imageUrls.addAll(newImageUrls);
            // 목록 썸네일도 첫 번째 이미지로 갱신 (없으면 null)
            this.thumbnailUrl = newImageUrls.isEmpty() ? null : newImageUrls.get(0);
        }
    }
    // ID 기반 비교로 영속성 컨텍스트에 의존하지 않음
    public boolean isAuthor(User user) {
        return this.user.getId().equals(user.getId());
    }
}
