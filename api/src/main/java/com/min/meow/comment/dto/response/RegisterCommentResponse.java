package com.min.meow.comment.dto.response;


import com.min.meow.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Schema(description = "댓글 등록 응답")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCommentResponse {

    @Schema(description = "댓글 ID", example = "1")
    private Long id;

    @Schema(description = "댓글 내용", example = "고양이가 정말 귀엽네요!")
    private String content;

    @Schema(description = "작성자 사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "작성자 닉네임", example = "홍길동")
    private String userNickname;

    @Schema(description = "읽음 여부", example = "false")
    private boolean isRead;

    @Schema(description = "작성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    public static RegisterCommentResponse toResponse(Comment comment){
        return RegisterCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContents())
                .userId(comment.getUser().getId())
                .userNickname(comment.getUser().getNickname())
                .isRead(comment.isRead())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
