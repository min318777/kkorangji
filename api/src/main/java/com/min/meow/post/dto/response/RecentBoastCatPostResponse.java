package com.min.meow.post.dto.response;

import com.min.meow.post.entity.BoastCatPost;
import com.querydsl.core.annotations.QueryProjection;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 최근 게시물 목록 조회용 경량 응답 DTO
 * - 메인 페이지에서 최근 게시물 20개를 보여줄 때 사용
 * - 필요한 필드만 포함하여 네트워크 트래픽 최소화
 * - QueryDSL Projection을 통해 DB에서 필요한 컬럼만 조회
 */
@Getter
@Builder
@NoArgsConstructor
public class RecentBoastCatPostResponse {

    private Long id;
    private String title;
    private String writer;
    private LocalDateTime createdAt;
    private int commentCount;
    private int likeCount;
    private int view;

    /**
     * QueryDSL Projection용 생성자
     * - @QueryProjection으로 Q클래스 자동 생성
     * - 컴파일 타임에 타입 체크 가능
     * - 필드 순서 변경 시 컴파일 에러로 안전하게 감지
     */
    @QueryProjection
    public RecentBoastCatPostResponse(Long id, String title, String writer,
                                       LocalDateTime createdAt, int commentCount,
                                       int likeCount, int view) {
        this.id = id;
        this.title = title;
        this.writer = writer;
        this.createdAt = createdAt;
        this.commentCount = commentCount;
        this.likeCount = likeCount;
        this.view = view;
    }

    /**
     * Entity -> DTO 변환 (기존 방식, 다른 곳에서 사용 시 유지)
     */
    public static RecentBoastCatPostResponse from(BoastCatPost post) {
        return RecentBoastCatPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .writer(post.getUser().getLoginId())
                .createdAt(post.getCreatedAt())
                .commentCount(post.getCommentCount())
                .likeCount(post.getLikeCount())
                .view(post.getView())
                .build();
    }
}
