package com.min.meow.post.comment.entity;


import com.min.meow.post.boastcatpost.entity.BoastCatPost;
import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.lostcatpost.entity.LostCatPost;
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
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //private Long postId;

    //private Long subComments;

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

    public void update(UpdateCommentRequest updateCommentRequest){

        if (updateCommentRequest.getContent() != null) {
            this.content = updateCommentRequest.getContent();
        }
    }

    public static Comment convertToEntity(RegisterCommentRequest registerCommentRequest, LostCatPost lostCatPost){
        return Comment.builder()
                .content(registerCommentRequest.getContent())
                .lostCatPost(lostCatPost)
                .build();
    }

}
