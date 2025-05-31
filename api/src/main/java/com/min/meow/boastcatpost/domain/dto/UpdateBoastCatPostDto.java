package com.min.meow.boastcatpost.domain.dto;

import com.min.meow.boastcatpost.entity.BoastCatPost;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBoastCatPostDto {

    private Long boastCatPostId;
    private String title;
    private String content;
    private String catImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static UpdateBoastCatPostDto convertToDto(BoastCatPost boastCatPost){
        return UpdateBoastCatPostDto.builder()
                .boastCatPostId(boastCatPost.getBoastCatPostId())
                .title(boastCatPost.getTitle())
                .content(boastCatPost.getContent())
                .catImageUrl(boastCatPost.getCatImageUrl())
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
