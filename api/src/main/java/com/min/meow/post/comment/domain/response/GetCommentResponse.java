package com.min.meow.post.comment.domain.response;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetCommentResponse {

    private Long id;
    private String content;
    private Long postId;
    private String loginId;
    //private int subCommentId;
    private LocalDateTime createdAt;
}
