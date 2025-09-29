package com.min.meow.post.domain.response;


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
public class GetBoastCatPostResponse {

    private Long id;

    private String writer;

    private String title;

    private String contents;

    private List<String> imageUrls = new ArrayList<>();

    //private List<LostCatPostCommentDto> lostCatPostCommentDtos;

    private Long userId;

    private int view;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static GetBoastCatPostResponse convertToResponse(BoastCatPost boastCatPost){

        return GetBoastCatPostResponse.builder()
                .id(boastCatPost.getId())
                .writer(boastCatPost.getUser().getLoginId())
                .title(boastCatPost.getTitle())
                .contents(boastCatPost.getContents())
                .imageUrls(new ArrayList<>(boastCatPost.getImageUrls()))
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
