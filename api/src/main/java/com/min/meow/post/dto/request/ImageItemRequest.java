package com.min.meow.post.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 수정 시 이미지 하나를 나타내는 요청 항목
 * - EXISTING: 유지할 기존 이미지 (value = CloudFront URL)
 * - NEW: 새로 업로드한 이미지 (value = S3 key)
 * 리스트 내 순서가 곧 최종 이미지 노출 순서
 */
@Schema(description = "게시글 이미지 항목 (기존/신규 이미지를 하나의 순서 리스트로 표현)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageItemRequest {

    @Schema(description = "이미지 종류", example = "EXISTING")
    @NotNull(message = "이미지 종류(type)는 필수입니다.")
    private ImageType type;

    @Schema(description = "EXISTING이면 CloudFront URL, NEW면 S3 key",
            example = "meow/uuid-new-1.jpg")
    @NotBlank(message = "이미지 값(value)은 비어있을 수 없습니다.")
    private String value;

    public enum ImageType {
        EXISTING,
        NEW
    }
}
