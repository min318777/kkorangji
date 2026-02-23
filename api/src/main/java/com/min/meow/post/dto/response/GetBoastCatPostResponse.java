package com.min.meow.post.dto.response;


import com.min.meow.post.entity.BoastCatPost;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "자랑글 상세 조회 응답")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetBoastCatPostResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "작성자 로그인 ID", example = "cat_lover")
    private String writer;

    @Schema(description = "작성자 사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "게시글 제목", example = "우리 고양이 자랑합니다")
    private String title;

    @Schema(description = "게시글 내용", example = "우리 고양이가 너무 귀여워요!")
    private String contents;

    @Schema(description = "이미지 URL 목록")
    private List<String> imageUrls;

    @Schema(description = "좋아요 수", example = "42")
    private int likeCount;

    @Schema(description = "댓글 수", example = "5")
    private int commentCount;

    @Schema(description = "조회수", example = "150")
    private int view;

    @Schema(description = "작성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-15T11:00:00")
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
