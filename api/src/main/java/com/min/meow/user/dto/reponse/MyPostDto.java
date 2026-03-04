package com.min.meow.user.dto.reponse;

import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 내가 쓴 게시글 정보 DTO
 * 고양이 자랑글과 실종 고양이글을 통합하여 표현
 */
@Schema(description = "내가 쓴 게시글 정보")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyPostDto {

    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "게시글 타입 (BOAST: 자랑글, LOST: 실종글)", example = "BOAST")
    private String postType;

    @Schema(description = "게시글 제목", example = "우리 고양이 자랑합니다")
    private String title;

    @Schema(description = "게시글 내용", example = "우리 고양이가 너무 귀여워요!")
    private String contents;

    @Schema(description = "조회수", example = "150")
    private int view;

    @Schema(description = "댓글 수", example = "5")
    private int commentCount;

    @Schema(description = "좋아요 수 (자랑글만 해당, 실종글은 null)", example = "42")
    private Integer likeCount;

    @Schema(description = "완료 여부 (실종글만 해당, 자랑글은 null)", example = "false")
    private Boolean isCompleted;

    @Schema(description = "작성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-15T11:00:00")
    private LocalDateTime updatedAt;

    /**
     * BoastCatPost 엔티티를 DTO로 변환
     */
    public static MyPostDto from(BoastCatPost post) {
        return MyPostDto.builder()
                .postId(post.getId())
                .postType("BOAST")
                .title(post.getTitle())
                .contents(post.getContents())
                .view(post.getView())
                .commentCount(post.getCommentCount())
                .likeCount(post.getLikeCount())
                .isCompleted(null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * LostCatPost 엔티티를 DTO로 변환
     */
    public static MyPostDto fromLostCatPost(LostCatPost post) {
        return MyPostDto.builder()
                .postId(post.getId())
                .postType("LOST")
                .title(post.getTitle())
                .contents(post.getContents())
                .view(post.getView())
                .commentCount(post.getCommentCount())
                .likeCount(null)  // 실종글은 좋아요 없음
                .isCompleted(post.isCompleted())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
