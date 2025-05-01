package com.min.meow.comment.entity;


import com.min.meow.comment.domain.request.RegisterCommentRequest;
import com.min.meow.lostcatpost.entity.LostCatPost;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
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


    public static LostCatPostComment convertToEntity(RegisterCommentRequest registerCommentRequest, LostCatPost lostCatPost){
        return LostCatPostComment.builder()
                .content(registerCommentRequest.getContent())
                .lostCatPost(lostCatPost)
                .build();
    }

}
