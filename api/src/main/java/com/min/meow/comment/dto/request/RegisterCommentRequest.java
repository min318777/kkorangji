package com.min.meow.comment.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Schema(description = "댓글 작성 요청")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterCommentRequest {

    @Schema(description = "댓글 내용 (1~500자)", example = "너무 귀여운 고양이네요!")
    @NotBlank(message = "댓글을 입력해 주세요.")
    @Size(min = 1, max = 500, message = "댓글은 1자 이상 500자 이하로 입력해주세요.")
    private String content;

    @Schema(description = "부모 댓글 ID (null = 원댓글, 값 있으면 대댓글)", example = "1")
    private Long parentCommentId;

}
