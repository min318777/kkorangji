package com.min.meow.post.domain.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBoastCatPostRequest {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;
    @Size(max = 1000, message = "내용은 1000자 이하로 입력해주세요.")
    private String content;
    @Size(max = 10, message = "이미지는 10장 이하로 업로드해주세요.")
    private List<MultipartFile> images;
}
