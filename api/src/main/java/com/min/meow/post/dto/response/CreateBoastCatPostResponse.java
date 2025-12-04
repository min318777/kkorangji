package com.min.meow.post.dto.response;


import com.min.meow.post.entity.BoastCatPost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBoastCatPostResponse {

    private Long id;
    private String title;
    private String content;
    private String writer;
    private List<String> imageUrls = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static CreateBoastCatPostResponse toResponse(BoastCatPost boastCatPost){
        return CreateBoastCatPostResponse.builder()
                .id(boastCatPost.getId())
                .title(boastCatPost.getTitle())
                .writer(boastCatPost.getUser().getLoginId())
                .content(boastCatPost.getContents())
                .imageUrls(boastCatPost.getImageUrls())
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
