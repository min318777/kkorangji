package com.min.meow.image.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Presigned URL 발급 요청 DTO
 *
 * 클라이언트가 업로드할 이미지의 Content-Type 목록을 전달하면
 * 해당 개수만큼 Presigned URL을 생성하여 반환합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {

    /**
     * 업로드할 이미지의 Content-Type 목록
     * 예: ["image/jpeg", "image/png"]
     */
    @NotEmpty(message = "Content-Type 목록은 필수입니다.")
    @Size(max = 10, message = "한 번에 최대 10개까지 요청 가능합니다.")
    private List<String> contentTypes;
}
