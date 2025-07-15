package com.min.meow.boastcatpost.entity;


import com.min.meow.boastcatpost.domain.request.CreateBoastCatPostRequest;
import com.min.meow.boastcatpost.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.global.BasePost;
import com.min.meow.postlike.entity.PostLike;
import com.min.meow.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
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

    @OneToMany
    private List<PostLike> postLikeList = new ArrayList<>();

    //private List<commentDto> commentDtos;

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
