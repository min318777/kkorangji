package com.min.meow.post.dto.response;


import com.min.meow.post.entity.BoastCatPost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 자랑글 상세 조회 응답 DTO
 *
 * 캐싱을 위해 정적 데이터만 포함:
 * - 댓글은 별도 API로 조회: GET /api/meow/boast-cat/comments/{postId}
 * - 조회수는 별도 API로 증가: POST /api/meow/boast-cat/{postId}/view
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetBoastCatPostResponse {

    private Long id;
    private String writer;
    private Long userId;
    private String title;
    private String contents;
    private List<String> imageUrls;
    private int likeCount;
    private int commentCount;
    private int view;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GetBoastCatPostResponse toResponse(BoastCatPost boastCatPost){
        return GetBoastCatPostResponse.builder()
                .id(boastCatPost.getId())
                .writer(boastCatPost.getUser().getLoginId())
                .userId(boastCatPost.getUser().getId())
                .title(boastCatPost.getTitle())
                .contents(boastCatPost.getContents())
                .view(boastCatPost.getView())
                .imageUrls(new ArrayList<>(boastCatPost.getImageUrls()))
                .likeCount(boastCatPost.getLikeCount())
                .commentCount(boastCatPost.getCommentCount())
                .createdAt(boastCatPost.getCreatedAt())
                .updatedAt(boastCatPost.getUpdatedAt())
                .build();
    }
}
