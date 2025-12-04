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
public class UpdateBoastCatPostResponse {

    private Long id;
    private String title;
    private String content;
    private List<String> imageUrls = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static UpdateBoastCatPostResponse convertToResponse(BoastCatPost boastCatPost){
        return UpdateBoastCatPostResponse.builder()
                .id(boastCatPost.getId())
                .title(boastCatPost.getTitle())
                .imageUrls(boastCatPost.getImageUrls())
                .content(boastCatPost.getContents())
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
