package com.min.meow.comment.dto.response;


import com.min.meow.post.comment.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCommentResponse {

    private Long id;
    private String content;
    private Long postId;
    private int subCommentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UpdateCommentResponse toResponse(Comment comment){
        return UpdateCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContents())
                .postId(comment.getLostCatPost().getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
