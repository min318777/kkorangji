package com.min.meow.post.comment.domain.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterCommentRequest {

    @NotBlank(message = "최소 5자 이상 작성해 주세요.")
    @Size(min = 5, max = 500, message = "최소 5자 이상, 최대 500자 이하 입력해 주세요.")
    private String content;
}
