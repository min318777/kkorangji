package com.min.meow.comment.dto.response;


import com.min.meow.post.comment.entity.Comment;
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
    private Long postId;
    private String writer;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static RegisterCommentResponse toResponse(Comment comment){
        return RegisterCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContents())
                .writer(comment.getWriter())
                .isRead(comment.isRead())
                .postId(comment.getBoastCatPost() != null ? comment.getBoastCatPost().getId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
