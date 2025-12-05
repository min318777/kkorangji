package com.min.meow.comment.dto;

import com.min.meow.post.comment.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;
    private String contents;
    private int subCommentId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .contents(comment.getContents())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
