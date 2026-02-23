package com.min.meow.post.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 실종 고양이 게시글 목록 조회용 경량 응답 DTO
 *
 * - 목록 페이지에서 필요한 필드만 포함
 * - contents, imageUrls, comments 등 불필요한 데이터 제외
 * - QueryDSL Projection으로 DB에서 필요한 컬럼만 SELECT
 *
 * 성능 개선 효과:
 * - 네트워크 트래픽 감소 (불필요한 데이터 전송 제거)
 * - DB 조회 최적화 (필요한 컬럼만 SELECT)
 * - Entity -> DTO 변환 오버헤드 제거
 * - LazyInitializationException 방지 (연관 엔티티 접근 없음)
 */
@Schema(description = "실종글 목록 조회 응답 (경량)")
@Getter
@Builder
@NoArgsConstructor
public class LostCatPostListResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 제목", example = "우리 고양이를 찾아주세요")
    private String title;

    @Schema(description = "작성자 로그인 ID", example = "cat_lover")
    private String writer;

    @Schema(description = "고양이 이름", example = "나비")
    private String catName;

    @Schema(description = "실종 장소", example = "서울시 강남구 역삼동")
    private String lostLocation;

    @Schema(description = "댓글 수", example = "5")
    private int commentCount;

    @Schema(description = "조회수", example = "150")
    private int view;

    @Schema(description = "찾음 여부", example = "false")
    private boolean isCompleted;

    @Schema(description = "작성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    /**
     * QueryDSL Projection용 생성자
     * @QueryProjection 어노테이션으로 Q클래스 자동 생성
     */
    @QueryProjection
    public LostCatPostListResponse(Long id, String title, String writer,
                                    String catName, String lostLocation,
                                    int commentCount, int view,
                                    boolean isCompleted, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.writer = writer;
        this.catName = catName;
        this.lostLocation = lostLocation;
        this.commentCount = commentCount;
        this.view = view;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }
}
