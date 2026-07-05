package com.min.meow.comment.dto.response;

import com.min.meow.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "댓글 조회 응답")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetCommentResponse {

    @Schema(description = "댓글 ID", example = "1")
    private Long id;

    @Schema(description = "부모 댓글 ID (null = 원댓글)", example = "null")
    private Long parentCommentId;

    @Schema(description = "댓글 내용 (삭제 시 '삭제된 댓글입니다.')", example = "고양이가 정말 귀엽네요!")
    private String contents;

    @Schema(description = "삭제 여부", example = "false")
    private boolean isDeleted;

    @Schema(description = "작성자 사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "작성자 로그인 ID", example = "cat_lover")
    private String loginId;

    @Schema(description = "작성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-15T11:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "대댓글 목록")
    private List<GetCommentResponse> replies;

    // 원댓글 변환 (대댓글 포함)
    public static GetCommentResponse toResponse(Comment comment, List<Comment> replies) {
        return GetCommentResponse.builder()
                .id(comment.getId())
                .parentCommentId(null)
                .contents(comment.getContents())
                .isDeleted(comment.isDeleted())
                .userId(comment.getUser().getId())
                .loginId(comment.getUser().getLoginId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies.stream().map(GetCommentResponse::toReplyResponse).toList())
                .build();
    }

    // 대댓글 변환
    public static GetCommentResponse toReplyResponse(Comment comment) {
        return GetCommentResponse.builder()
                .id(comment.getId())
                .parentCommentId(comment.getParentComment().getId())
                .contents(comment.getContents())
                .isDeleted(comment.isDeleted())
                .userId(comment.getUser().getId())
                .loginId(comment.getUser().getLoginId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(List.of())
                .build();
    }
}