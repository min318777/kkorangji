package com.min.meow.post.comment.domain.response;

import com.min.meow.post.comment.entity.Comment;
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
    private Long postId;
    private String writer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GetCommentResponse fromEntity(Comment comment){
        return GetCommentResponse.builder()
                .id(comment.getId())
                .contents(comment.getContents())
                .writer(comment.getWriter())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
