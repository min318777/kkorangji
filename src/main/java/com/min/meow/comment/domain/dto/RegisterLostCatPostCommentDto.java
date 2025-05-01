package com.min.meow.comment.domain.dto;


import com.min.meow.comment.entity.LostCatPostComment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterLostCatPostCommentDto {

    private Long lostCatPostCommentId;
    private String content;
    private Long lostCatPostId;
    private LocalDateTime createdAt;

    public static RegisterLostCatPostCommentDto convertToDto(LostCatPostComment lostCatPostComment){
        return RegisterLostCatPostCommentDto.builder()
                .lostCatPostCommentId(lostCatPostComment.getLostCatPostCommentId())
                .content(lostCatPostComment.getContent())
                .lostCatPostId(lostCatPostComment.getLostCatPost().getLostCatPostId())
                .createdAt(lostCatPostComment.getCreatedAt())
                .build();

    }
}
