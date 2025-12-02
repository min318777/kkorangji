package com.min.meow.post.domain.request;


import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLostCatPostRequest {

    @Size(min = 2, max = 100, message = "제목은 2자이상 100자이하로 입력해주세요.")
    private String title;
    private String content;
    private String catName;
    private String catType;
    private String catColor;
    private Integer catAge;
    private Integer catWeight;
    private String lostLocation;
    private Double latitude;
    private Double longitude;
    private Integer reward;
    private boolean isCompleted;
    private List<MultipartFile> newImages;
    private List<String> keepImageUrls;
    private List<String> deleteImageUrls;
}
