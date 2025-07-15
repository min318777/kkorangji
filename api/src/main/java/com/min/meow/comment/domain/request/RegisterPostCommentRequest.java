package com.min.meow.comment.domain.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegisterPostCommentRequest {

    @NotBlank(message = "최소 다섯글자 이상 작성해 주세요.")
    @Size(min = 2, max = 500, message = "최소 2자 이상, 최대 500자 이하 입력해 주세요.")
    private String content;
}
