package com.min.meow.boastcatpost.domain.request;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBoastCatPostRequest {

    @NotBlank(message = "제목을 입력해 주세요.")
    private String title;
    private String content;
    private String catImageUrl;
}
