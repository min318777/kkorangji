package com.min.meow.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Presigned URL 발급 요청 DTO
 * 클라이언트가 업로드할 파일들의 (Content-Type, 파일 크기) 목록을 전달하면
 * 해당 개수만큼 Presigned URL을 생성하여 반환합니다.
 */
@Schema(description = "Presigned URL 발급 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {

    @Schema(description = "업로드할 파일 목록 (이미지 최대 10개 + 동영상 최대 1개)")
    @NotEmpty(message = "파일 목록은 필수입니다.")
    @Size(max = 11, message = "한 번에 최대 11개(이미지 10 + 동영상 1)까지 요청 가능합니다.")
    @Valid
    private List<PresignedFileRequest> files;
}
