package com.min.meow.comment.entity;


import com.min.meow.comment.domain.request.RegisterLostCatPostCommentRequest;
import com.min.meow.comment.domain.request.UpdateLostCatPostCommentRequest;
import com.min.meow.lostcatpost.entity.LostCatPost;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostCatPostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lostCatPostCommentId;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_cat_post_id")
    private LostCatPost lostCatPost;

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

    public void update(UpdateLostCatPostCommentRequest updateLostCatPostCommentRequest){

        if (updateLostCatPostCommentRequest.getContent() != null) {
            this.content = updateLostCatPostCommentRequest.getContent();
        }
    }

    public static LostCatPostComment convertToEntity(RegisterLostCatPostCommentRequest registerLostCatPostCommentRequest, LostCatPost lostCatPost){
        return LostCatPostComment.builder()
                .content(registerLostCatPostCommentRequest.getContent())
                .lostCatPost(lostCatPost)
                .build();
    }

}
