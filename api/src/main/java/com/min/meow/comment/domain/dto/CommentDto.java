package com.min.meow.comment.domain.dto;

import com.min.meow.comment.entity.PostComment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long postId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentDto convertToDto(PostComment postComment) {
        return CommentDto.builder()
                .postId(postComment.getPostCommentId())
                .content(postComment.getContent())
                .createdAt(postComment.getCreatedAt())
                .updatedAt(postComment.getUpdatedAt())
                .build();
    }
}
