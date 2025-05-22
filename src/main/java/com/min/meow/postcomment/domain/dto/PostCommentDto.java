package com.min.meow.postcomment.domain.dto;

import com.min.meow.postcomment.entity.PostComment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCommentDto {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostCommentDto convertToDto(PostComment postComment) {
        return PostCommentDto.builder()
                .id(postComment.getPostCommentId())
                .content(postComment.getContent())
                .createdAt(postComment.getCreatedAt())
                .updatedAt(postComment.getUpdatedAt())
                .build();
    }
}
