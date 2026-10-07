package com.min.meow.post.dto.response;

import com.min.meow.post.entity.BoastCatPost;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Schema(description = "자랑글 목록 조회 응답 (경량)")
@Getter
@Builder
@NoArgsConstructor
public class BoastCatPostListResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 제목", example = "우리 고양이 자랑합니다")
    private String title;

    @Schema(description = "좋아요 수", example = "42")
    private int likeCount;

    @Schema(description = "댓글 수", example = "5")
    private int commentCount;

    @Schema(description = "조회수", example = "150")
    private int view;

    @Schema(description = "작성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "목록 썸네일 URL (첫 번째 이미지, 없으면 null)", example = "https://cdn.example.com/img.jpg")
    private String thumbnailUrl;

    @QueryProjection
    public BoastCatPostListResponse(Long id, String title, int likeCount, int commentCount, int view, LocalDateTime createdAt, String thumbnailUrl) {
        this.id = id;
        this.title = title;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.view = view;
        this.createdAt = createdAt;
        this.thumbnailUrl = thumbnailUrl;
    }

    // JPA 메서드 이름 쿼리(LIKE 검색)로 조회한 엔티티를 DTO로 변환
    public static BoastCatPostListResponse from(BoastCatPost post) {
        return BoastCatPostListResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .view(post.getView())
                .createdAt(post.getCreatedAt())
                .thumbnailUrl(post.getThumbnailUrl())
                .build();
    }
}
