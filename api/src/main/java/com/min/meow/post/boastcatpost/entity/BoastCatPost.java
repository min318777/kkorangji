package com.min.meow.post.boastcatpost.entity;


import com.min.meow.post.boastcatpost.domain.request.CreateBoastCatPostRequest;
import com.min.meow.post.boastcatpost.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.global.BasePost;
import com.min.meow.post.postlike.entity.PostLike;
import com.min.meow.post.search.domain.PostDto;
import com.min.meow.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BoastCatPost extends BasePost {

    private String catImageUrl;

    @OneToMany(mappedBy = "boastCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikeList = new ArrayList<>();

    //private List<commentDto> commentDtos;

    public void update(UpdateBoastCatPostRequest updateBoastCatPostRequest){
        if (updateBoastCatPostRequest.getTitle() != null) {
            this.title = updateBoastCatPostRequest.getTitle();
        }
        if (updateBoastCatPostRequest.getContent() != null) {
            this.contents = updateBoastCatPostRequest.getContent();
        }
        if (updateBoastCatPostRequest.getCatImageUrl() != null) {
            this.catImageUrl = updateBoastCatPostRequest.getCatImageUrl();
        }
    }

    public static BoastCatPost convertToEntity(CreateBoastCatPostRequest createBoastCatPostRequest, User writer){

        return BoastCatPost.builder()
                .title(createBoastCatPostRequest.getTitle())
                .contents(createBoastCatPostRequest.getContent())
                .user(writer)
                .catImageUrl(createBoastCatPostRequest.getCatImageUrl())
                .build();
    }

    public static PostDto toDto(BoastCatPost boastCatPost){
        return new PostDto().builder()
                .id(boastCatPost.getId())
                .title(boastCatPost.getTitle())
                .contents(boastCatPost.getContents())
                .userId(boastCatPost.getUser().getId())
                .view(boastCatPost.getView())
                .createdAt(boastCatPost.createdAt)
                .updatedAt(boastCatPost.updatedAt)
                .build();
    }
}
