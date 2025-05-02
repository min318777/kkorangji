package com.min.meow.comment.domain.dto;

import com.min.meow.comment.entity.LostCatPostComment;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostCatPostCommentDto {

    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LostCatPostCommentDto convertToDto(LostCatPostComment lostCatPostComment) {
        return LostCatPostCommentDto.builder()
                .id(lostCatPostComment.getLostCatPostCommentId())
                .content(lostCatPostComment.getContent())
                .createdAt(lostCatPostComment.getCreatedAt())
                .updatedAt(lostCatPostComment.getUpdatedAt())
                .build();
    }
}
