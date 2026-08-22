package com.min.meow.post.dto.response;


import com.min.meow.post.entity.BoastCatPost;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "자랑글 작성 응답")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBoastCatPostResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 제목", example = "우리 고양이 자랑합니다")
    private String title;

    @Schema(description = "게시글 내용", example = "우리 고양이가 너무 귀여워요!")
    private String content;

    @Schema(description = "작성자 로그인 ID", example = "cat_lover")
    private String writer;

    @Schema(description = "이미지 URL 목록")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Schema(description = "작성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-15T10:30:00")
    private LocalDateTime updatedAt;


    public static CreateBoastCatPostResponse from(BoastCatPost boastCatPost){
        return CreateBoastCatPostResponse.builder()
                .id(boastCatPost.getId())
                .title(boastCatPost.getTitle())
                .writer(boastCatPost.getUser().getNickname())
                .content(boastCatPost.getContents())
                .imageUrls(boastCatPost.getImageUrls())
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
