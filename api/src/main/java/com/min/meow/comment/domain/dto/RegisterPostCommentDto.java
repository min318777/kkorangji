package com.min.meow.comment.domain.dto;


import com.min.meow.comment.entity.PostComment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPostCommentDto {

    private Long lostCatPostCommentId;
    private String content;
    private Long lostCatPostId;
    private String loginId;
    private LocalDateTime createdAt;

    public static RegisterPostCommentDto convertToDto(PostComment postComment){
        return RegisterPostCommentDto.builder()
                .lostCatPostCommentId(postComment.getPostCommentId())
                .content(postComment.getContent())
                .lostCatPostId(postComment.getLostCatPost().getId())
                .createdAt(postComment.getCreatedAt())
                .build();

    }
}
