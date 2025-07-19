package com.min.meow.post.boastcatpost.domain.response;


import com.min.meow.post.boastcatpost.entity.BoastCatPost;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBoastCatPostResponse {

    private Long id;
    private String title;
    private String content;
    private String catImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static CreateBoastCatPostResponse convertToDto(BoastCatPost boastCatPost){
        return CreateBoastCatPostResponse.builder()
                .id(boastCatPost.getId())
                .title(boastCatPost.getTitle())
                .content(boastCatPost.getContents())
                .catImageUrl(boastCatPost.getCatImageUrl())
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
