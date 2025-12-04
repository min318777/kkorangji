package com.min.meow.post.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLostCatPostRequest {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(min = 2, max = 100, message = "최소 2자 이상, 최대 100자 이하로 입력해주세요.")
    private String title;
    @NotBlank
    @Size(min = 2, max = 500, message = "최소 2자 이상, 최대 1000자 이하로 입력해주세요.")
    private String content;
    private String catName;
    private String catType;
    private String catColor;
    @Min(value = 0, message = "0이상의 값을 입력해주세요.")
    private Integer catAge;
    @Min(value = 0, message = "0이상의 값을 입력해주세요.")
    private Integer catWeight;
    private String lostLocation;
    private Double latitude;
    private Double longitude;
    private Integer reward;
    @Size(max = 10, message = "이미지는 10장 이하로 업로드해주세요.")
    private List<MultipartFile> images;

}
