package com.min.meow.image.controller;

import com.min.meow.config.S3Service;
import com.min.meow.common.ApiResponse;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.image.dto.request.PresignedUrlRequest;
import com.min.meow.image.dto.response.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "이미지", description = "S3 Presigned URL 발급 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final S3Service s3Service;

    @Operation(summary = "Presigned URL 여러개 발급",
            description = "업로드할 이미지 개수만큼 S3 Presigned URL을 발급합니다. "
                    + "허용 형식: image/jpeg, image/jpg, image/png, image/gif, image/webp. 인증 필요.")
    @PostMapping("/presigned-urls")
    public ResponseEntity<ApiResponse<List<PresignedUrlResponse>>> getPresignedUrls(
            @Valid @RequestBody PresignedUrlRequest request) {

        log.info("Presigned URL 요청 - 개수: {}", request.getContentTypes().size());

        // Content-Type 검증
        validateContentTypes(request.getContentTypes());

        // Presigned URL 생성
        List<PresignedUrlResponse> responses = s3Service.generatePresignedUrls(request.getContentTypes())
                .stream()
                .map(PresignedUrlResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Presigned URL 발급 성공", responses));
    }

    @Operation(summary = "Presigned URL 단건 발급",
            description = "단일 이미지 업로드용 Presigned URL을 발급합니다. "
                    + "허용 형식: image/jpeg, image/jpg, image/png, image/gif, image/webp. 인증 필요. "
                    + "Body: { \"contentType\": \"image/jpeg\" }")
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestBody Map<String, String> body) {

        String contentType = body.get("contentType");
        log.info("단일 Presigned URL 요청 - contentType: {}", contentType);

        validateContentType(contentType);

        S3Service.PresignedUrlInfo urlInfo = s3Service.generatePresignedUrl(contentType);
        PresignedUrlResponse response = PresignedUrlResponse.from(urlInfo);

        return ResponseEntity.ok(ApiResponse.success("Presigned URL 발급 성공", response));
    }

    /**
     * Content-Type 목록 검증
     */
    private void validateContentTypes(List<String> contentTypes) {
        for (String contentType : contentTypes) {
            validateContentType(contentType);
        }
    }

    /**
     * Content-Type 검증
     * 허용: image/jpeg, image/jpg, image/png, image/gif, image/webp
     */
    private void validateContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FORMAT, "Content-Type이 필요합니다");
        }

        List<String> allowedTypes = List.of(
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
        );

        if (!allowedTypes.contains(contentType.toLowerCase())) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FORMAT,
                    "요청된 형식: " + contentType);
        }
    }
}
