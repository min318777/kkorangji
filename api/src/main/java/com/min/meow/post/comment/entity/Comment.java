package com.min.meow.post.comment.entity;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.entity.LostCatPost;
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

    private String contents;

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
            this.contents = updateCommentRequest.getContent();
        }
    }

    public static Comment convertToEntity(RegisterCommentRequest registerCommentRequest, LostCatPost lostCatPost){
        return Comment.builder()
                .contents(registerCommentRequest.getContent())
                .lostCatPost(lostCatPost)
                .build();
    }

}
