package com.min.meow.comment.dto.response;


import com.min.meow.comment.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCommentResponse {

    private Long id;
    private String content;
    private Long userId;
    private String userName;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static RegisterCommentResponse toResponse(Comment comment){
        return RegisterCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContents())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getName())
                .isRead(comment.isRead())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
