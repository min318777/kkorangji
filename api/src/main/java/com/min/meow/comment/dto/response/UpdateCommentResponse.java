package com.min.meow.comment.dto.response;


import com.min.meow.comment.entity.Comment;
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
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UpdateCommentResponse toResponse(Comment comment){
        return UpdateCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContents())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getName())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
