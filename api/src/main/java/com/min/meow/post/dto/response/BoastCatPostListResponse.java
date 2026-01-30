package com.min.meow.post.dto.response;

import com.querydsl.core.annotations.QueryProjection;
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
@Getter
@Builder
@NoArgsConstructor
public class BoastCatPostListResponse {

    private Long id;
    private String title;
    private String writer;
    private int likeCount;
    private int commentCount;
    private int view;
    private LocalDateTime createdAt;

    @QueryProjection
    public BoastCatPostListResponse(Long id, String title, String writer,
                                     int likeCount, int commentCount,
                                     int view, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.writer = writer;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.view = view;
        this.createdAt = createdAt;
    }
}
