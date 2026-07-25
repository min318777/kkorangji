package com.min.meow.comment.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Schema(description = "댓글 수정 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCommentRequest {

    @Schema(description = "수정할 댓글 내용 (1~500자)", example = "수정된 댓글입니다.")
    @NotBlank(message = "댓글을 입력해 주세요.")
    @Size(min = 1, max = 500, message = "댓글은 1자 이상 500자 이하로 입력해주세요.")
    private String content;
}
