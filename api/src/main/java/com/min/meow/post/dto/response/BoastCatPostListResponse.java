package com.min.meow.post.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 목록 조회용 경량 응답 DTO
 * - 목록 페이지에서 필요한 필드만 포함
 * - contents, imageUrls, comments 등 불필요한 데이터 제외
 * - QueryDSL Projection으로 DB에서 필요한 컬럼만 SELECT
 *
 * 성능 개선 효과:
 * - 네트워크 트래픽 감소 (불필요한 데이터 전송 제거)
 * - DB 조회 최적화 (필요한 컬럼만 SELECT)
 * - Entity -> DTO 변환 오버헤드 제거
 */
@Schema(description = "자랑글 목록 조회 응답 (경량)")
@Getter
@Builder
@NoArgsConstructor
public class BoastCatPostListResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 제목", example = "우리 고양이 자랑합니다")
    private String title;

    @Schema(description = "작성자 로그인 ID", example = "cat_lover")
    private String writer;

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
    public BoastCatPostListResponse(Long id, String title, String writer,
                                     int likeCount, int commentCount,
                                     int view, LocalDateTime createdAt,
                                     String thumbnailUrl) {
        this.id = id;
        this.title = title;
        this.writer = writer;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.view = view;
        this.createdAt = createdAt;
        this.thumbnailUrl = thumbnailUrl;
    }
}
