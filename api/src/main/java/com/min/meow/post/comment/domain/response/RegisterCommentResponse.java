package com.min.meow.post.comment.domain.response;


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
    private String loginId;
    private LocalDateTime createdAt;

    public static RegisterCommentResponse convertToResponse(Comment comment){
        return RegisterCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContents())
                .postId(comment.getLostCatPost().getId())
                .createdAt(comment.getCreatedAt())
                .build();

    }
}
