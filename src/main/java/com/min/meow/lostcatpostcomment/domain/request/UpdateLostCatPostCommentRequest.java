package com.min.meow.lostcatpostcomment.domain.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateLostCatPostCommentRequest {

    private String content;
}
