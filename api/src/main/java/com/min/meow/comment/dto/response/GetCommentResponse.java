package com.min.meow.comment.dto.response;

import com.min.meow.comment.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetCommentResponse {

    private Long id;
    private String contents;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GetCommentResponse toResponse(Comment comment){
        return GetCommentResponse.builder()
                .id(comment.getId())
                .contents(comment.getContents())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getName())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
