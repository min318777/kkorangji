package com.min.meow.post.comment.domain.response;


import com.min.meow.post.comment.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCommentDto {

    private Long id;
    private String content;
    private Long lostCatPostId;
    private String loginId;
    private LocalDateTime createdAt;

    public static RegisterCommentDto convertToDto(Comment comment){
        return RegisterCommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .lostCatPostId(comment.getLostCatPost().getId())
                .createdAt(comment.getCreatedAt())
                .build();

    }
}
