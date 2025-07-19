package com.min.meow.post.boastcatpost.domain.request;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBoastCatPostRequest {

    @NotBlank(message = "제목을 입력해 주세요.")
    private String title;
    private String content;
    private String catImageUrl;
}
