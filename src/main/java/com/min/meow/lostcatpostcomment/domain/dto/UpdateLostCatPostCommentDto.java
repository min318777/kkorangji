package com.min.meow.lostcatpostcomment.domain.dto;


import com.min.meow.lostcatpostcomment.entity.LostCatPostComment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
public class UpdateLostCatPostCommentDto {

    private Long lostCatPostCommentId;
    private String content;
    private Long lostCatPostId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UpdateLostCatPostCommentDto convertToDto(LostCatPostComment lostCatPostComment){
        return UpdateLostCatPostCommentDto.builder()
                .lostCatPostCommentId(lostCatPostComment.getLostCatPostCommentId())
                .content(lostCatPostComment.getContent())
                .lostCatPostId(lostCatPostComment.getLostCatPost().getLostCatPostId())
                .createdAt(lostCatPostComment.getCreatedAt())
                .updatedAt(lostCatPostComment.getUpdatedAt())
                .build();
    }
}
