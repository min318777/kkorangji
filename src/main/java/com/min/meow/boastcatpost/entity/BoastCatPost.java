package com.min.meow.boastcatpost.entity;


import com.min.meow.boastcatpost.domain.request.CreateBoastCatPostRequest;
import com.min.meow.boastcatpost.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.global.BasePost;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import com.min.meow.postcomment.domain.dto.PostCommentDto;
import com.min.meow.postcomment.entity.PostComment;
import com.min.meow.postlike.entity.PostLike;
import com.min.meow.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BoastCatPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boastCatPostId;

    private String title;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String catImageUrl;

    @OneToMany
    private List<PostLike> postLikeList = new ArrayList<>();

    //private List<PostCommentDto> postCommentDtos;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    public void update(UpdateBoastCatPostRequest updateBoastCatPostRequest){
        if (updateBoastCatPostRequest.getTitle() != null) {
            this.title = updateBoastCatPostRequest.getTitle();
        }
        if (updateBoastCatPostRequest.getContent() != null) {
            this.content = updateBoastCatPostRequest.getContent();
        }
        if (updateBoastCatPostRequest.getCatImageUrl() != null) {
            this.catImageUrl = updateBoastCatPostRequest.getCatImageUrl();
        }
    }

    public static BoastCatPost convertToEntity(CreateBoastCatPostRequest createBoastCatPostRequest, User writer){

        return BoastCatPost.builder()
                .title(createBoastCatPostRequest.getTitle())
                .content(createBoastCatPostRequest.getContent())
                .user(writer)
                .catImageUrl(createBoastCatPostRequest.getCatImageUrl())
                .build();
    }
}
