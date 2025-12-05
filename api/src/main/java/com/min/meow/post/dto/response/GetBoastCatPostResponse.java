package com.min.meow.post.dto.response;


import com.min.meow.comment.dto.CommentDto;
import com.min.meow.post.entity.BoastCatPost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetBoastCatPostResponse {

    private Long id;

    private String writer;

    private String title;

    private String contents;

    private List<String> imageUrls = new ArrayList<>();

    private List<CommentDto> commentDtoList;

    private Long userId;

    private int view;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static GetBoastCatPostResponse toResponse(BoastCatPost boastCatPost){

        return GetBoastCatPostResponse.builder()
                .id(boastCatPost.getId())
                .view(boastCatPost.getView())
                .commentDtoList(boastCatPost.getComments().stream().map(CommentDto::toDto).toList())
                .writer(boastCatPost.getUser().getLoginId())
                .title(boastCatPost.getTitle())
                .contents(boastCatPost.getContents())
                .imageUrls(new ArrayList<>(boastCatPost.getImageUrls()))
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
