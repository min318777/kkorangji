package com.min.meow.comment.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentRequest {

    // 댓글: 1~500자, 필수
    @NotBlank(message = "댓글을 입력해 주세요.")
    @Size(min = 1, max = 500, message = "댓글은 1자 이상 500자 이하로 입력해주세요.")
    private String content;
}