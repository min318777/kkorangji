package com.min.meow.post.comment.domain.response;


import com.min.meow.post.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
public class UpdateCommentDto {

    private Long id;
    private String content;
    private Long lostCatPostId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UpdateCommentDto convertToDto(Comment comment){
        return UpdateCommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .lostCatPostId(comment.getLostCatPost().getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
