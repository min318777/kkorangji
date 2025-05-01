package com.min.meow.comment.domain.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegisterLostCatPostCommentRequest {


    @NotBlank(message = "최소 다섯글자 이상 작성해 주세요.")
    @Size(min = 5)
    private String content;
}
