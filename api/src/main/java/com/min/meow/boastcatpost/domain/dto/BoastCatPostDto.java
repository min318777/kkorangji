package com.min.meow.boastcatpost.domain.dto;


import com.min.meow.boastcatpost.entity.BoastCatPost;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoastCatPostDto {

    private Long boastCatPostId;

    private String title;

    private String content;

    private String catImageUrl;

    //private List<LostCatPostCommentDto> lostCatPostCommentDtos;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static BoastCatPostDto convertToDto(BoastCatPost boastCatPost){

        return BoastCatPostDto.builder()
                .boastCatPostId(boastCatPost.getBoastCatPostId())
                .title(boastCatPost.getTitle())
                .content(boastCatPost.getContent())
                .catImageUrl(boastCatPost.getCatImageUrl())
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
