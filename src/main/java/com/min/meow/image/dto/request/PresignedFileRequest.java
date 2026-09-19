package com.min.meow.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "업로드할 파일 1개의 정보")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedFileRequest {

    @Schema(description = "파일 Content-Type. 허용: image/jpeg, image/jpg, image/png, image/gif, image/webp, video/mp4",
            example = "image/jpeg")
    @NotBlank(message = "Content-Type은 필수입니다.")
    private String contentType;

    @Schema(description = "파일 크기(byte). 이미지 최대 10MB, 동영상 최대 200MB", example = "3145728")
    @NotNull(message = "파일 크기는 필수입니다.")
    @Positive(message = "파일 크기는 0보다 커야 합니다.")
    private Long fileSize;
}
