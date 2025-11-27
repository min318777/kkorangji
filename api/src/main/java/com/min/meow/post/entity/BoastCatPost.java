package com.min.meow.post.entity;


import com.min.meow.global.BasePost;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.comment.entity.Comment;
import com.min.meow.post.domain.request.CreateBoastCatPostRequest;
import com.min.meow.post.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.post.postlike.entity.PostLike;
import com.min.meow.post.search.domain.PostDto;
import com.min.meow.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BoastCatPost extends BasePost {

    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    @OneToMany(mappedBy = "boastCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLikeList = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "boastCatPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public void update(UpdateBoastCatPostRequest updateBoastCatPostRequest){
        if (updateBoastCatPostRequest.getTitle() != null) {
            this.title = updateBoastCatPostRequest.getTitle();
        }
        if (updateBoastCatPostRequest.getContent() != null) {
            this.contents = updateBoastCatPostRequest.getContent();
        }
    }

    public static BoastCatPost toEntity(CreateBoastCatPostRequest createBoastCatPostRequest, List<String> imageUrls, User writer){

        return BoastCatPost.builder()
                .title(createBoastCatPostRequest.getTitle())
                .contents(createBoastCatPostRequest.getContent())
                .imageUrls(imageUrls)
                .user(writer)
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

    public void plusView(){
        this.view += 1;
    }

    public boolean isAuthor(User user){
        return this.user.equals(user);
    }
    public void validateAuthor(User user){
        if (!isAuthor(user)){
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
    }
}
