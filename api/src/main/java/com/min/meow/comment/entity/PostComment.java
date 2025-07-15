package com.min.meow.comment.entity;


import com.min.meow.boastcatpost.entity.BoastCatPost;
import com.min.meow.comment.domain.request.RegisterPostCommentRequest;
import com.min.meow.comment.domain.request.UpdatePostCommentRequest;
import com.min.meow.lostcatpost.entity.LostCatPost;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postCommentId;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_cat_post_id")
    private LostCatPost lostCatPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boast_cat_post_id")
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

    public void update(UpdatePostCommentRequest updatePostCommentRequest){

        if (updatePostCommentRequest.getContent() != null) {
            this.content = updatePostCommentRequest.getContent();
        }
    }

    public static PostComment convertToEntity(RegisterPostCommentRequest registerPostCommentRequest, LostCatPost lostCatPost){
        return PostComment.builder()
                .content(registerPostCommentRequest.getContent())
                .lostCatPost(lostCatPost)
                .build();
    }

}
