package com.min.meow.user.dto.response;

import com.min.meow.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 내가 쓴 댓글 정보 DTO
 * 댓글이 달린 게시글 정보도 함께 포함
 */
@Schema(description = "내가 쓴 댓글 정보")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyCommentDto {

    @Schema(description = "댓글 ID", example = "1")
    private Long commentId;

    @Schema(description = "댓글 내용", example = "고양이가 정말 귀엽네요!")
    private String contents;

    @Schema(description = "댓글이 달린 게시글 ID", example = "5")
    private Long postId;

    @Schema(description = "게시글 타입 (BOAST: 자랑글, LOST: 실종글)", example = "BOAST")
    private String postType;

    @Schema(description = "게시글 제목", example = "우리 고양이 자랑합니다")
    private String postTitle;

    @Schema(description = "작성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-15T11:00:00")
    private LocalDateTime updatedAt;

    // postTitle은 MyPageService에서 배치 조회 후 주입
    public static MyCommentDto from(Comment comment, String postTitle) {
        return MyCommentDto.builder()
                .commentId(comment.getId())
                .contents(comment.getContents())
                .postId(comment.getPostId())
                .postType(comment.getPostType().name())
                .postTitle(postTitle)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
