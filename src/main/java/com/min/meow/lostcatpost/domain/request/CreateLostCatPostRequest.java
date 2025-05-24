package com.min.meow.lostcatpost.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLostCatPostRequest {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(min = 2, max = 30, message = "최소 2자 이상, 최대 30자 이하 입력해 주세요.")
    private String title;

    @NotBlank
    @Size(min = 2, max = 500, message = "최소 2자 이상, 최대 500자 이하 입력해 주세요.")
    private String content;

    private String catName;

    private String catType;

    private String catColor;

    private Integer catAge;

    private Integer catWeight;

    private String catImageUrl;

    private String lostLocation;

    private Double latitude;

    private Double longitude;

    private Integer reward;

}
