package com.min.meow.post.domain.response;


import com.min.meow.post.entity.BoastCatPost;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetBoastCatPostResponse {

    private Long id;

    private String title;

    private String content;

    private String catImageUrl;

    //private List<LostCatPostCommentDto> lostCatPostCommentDtos;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static GetBoastCatPostResponse convertToResponse(BoastCatPost boastCatPost){

        return GetBoastCatPostResponse.builder()
                .id(boastCatPost.getId())
                .title(boastCatPost.getTitle())
                .content(boastCatPost.getContents())
                .catImageUrl(boastCatPost.getCatImageUrl())
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
