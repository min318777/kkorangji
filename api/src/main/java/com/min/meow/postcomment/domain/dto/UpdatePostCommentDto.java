package com.min.meow.postcomment.domain.dto;


import com.min.meow.postcomment.entity.PostComment;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
public class UpdatePostCommentDto {

    private Long lostCatPostCommentId;
    private String content;
    private Long lostCatPostId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UpdatePostCommentDto convertToDto(PostComment postComment){
        return UpdatePostCommentDto.builder()
                .lostCatPostCommentId(postComment.getPostCommentId())
                .content(postComment.getContent())
                .lostCatPostId(postComment.getLostCatPost().getLostCatPostId())
                .createdAt(postComment.getCreatedAt())
                .updatedAt(postComment.getUpdatedAt())
                .build();
    }
}
