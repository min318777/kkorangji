package com.min.meow.postlike.entity;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "boast_cat_post_id"})})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK 삭제 전략: 사용자 삭제 시 좋아요가 존재하면 삭제 차단
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_post_like_user",
            foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE RESTRICT"))
    private User user;

    // FK 삭제 전략: 자랑 게시글 삭제 시 관련 좋아요도 함께 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boast_cat_post_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_post_like_boast_cat_post",
            foreignKeyDefinition = "FOREIGN KEY (boast_cat_post_id) REFERENCES boast_cat_post(id) ON DELETE CASCADE"))
    private BoastCatPost boastCatPost;



}
