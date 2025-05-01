package com.min.meow.comment.domain.request;


import jakarta.persistence.Column;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterCommentRequest {


    @NotBlank(message = "최소 다섯글자 이상 작성해 주세요.")
    @Size(min = 5)
    private String content;
}
