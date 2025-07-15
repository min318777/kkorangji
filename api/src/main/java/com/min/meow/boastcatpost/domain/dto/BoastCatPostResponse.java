package com.min.meow.boastcatpost.domain.dto;


import com.min.meow.boastcatpost.entity.BoastCatPost;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoastCatPostResponse {

    private Long id;

    private String title;

    private String content;

    private String catImageUrl;

    //private List<LostCatPostCommentDto> lostCatPostCommentDtos;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static BoastCatPostResponse convertToDto(BoastCatPost boastCatPost){

        return BoastCatPostResponse.builder()
                .id(boastCatPost.getId())
                .title(boastCatPost.getTitle())
                .content(boastCatPost.getContent())
                .catImageUrl(boastCatPost.getCatImageUrl())
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
