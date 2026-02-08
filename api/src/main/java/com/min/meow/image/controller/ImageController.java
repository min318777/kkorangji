package com.min.meow.image.controller;

import com.min.meow.config.S3Service;
import com.min.meow.global.ApiResponse;
import com.min.meow.image.dto.request.PresignedUrlRequest;
import com.min.meow.image.dto.response.PresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 이미지 업로드용 Presigned URL 발급 API
 *
 * 클라이언트 사용 플로우:
 * 1. POST /api/images/presigned-urls 로 Presigned URL 요청
 * 2. 응답받은 presignedUrl로 PUT 요청하여 S3에 직접 업로드
 * 3. 업로드 완료 후 key를 게시글 생성 API에 전달
 *
 * 장점:
 * - 서버 트래픽 비용 절감 (이미지가 서버를 거치지 않음)
 * - 서버 메모리 부담 감소
 * - 대용량 파일 업로드 가능 (서버 타임아웃 걱정 없음)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final S3Service s3Service;

    /**
     * Presigned URL 발급
     *
     * 요청 예시:
     * POST /api/images/presigned-urls
     * {
     *   "contentTypes": ["image/jpeg", "image/png"]
     * }
     *
     * 응답 예시:
     * {
     *   "status": "OK",
     *   "message": "Presigned URL 발급 성공",
     *   "data": [
     *     { "presignedUrl": "https://...", "key": "meow/uuid-1.jpg" },
     *     { "presignedUrl": "https://...", "key": "meow/uuid-2.png" }
     *   ]
     * }
     */
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

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Presigned URL 발급 성공", responses));
    }

    /**
     * 단일 Presigned URL 발급 (간편 API)
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam String contentType) {

        log.info("단일 Presigned URL 요청 - contentType: {}", contentType);

        validateContentType(contentType);

        S3Service.PresignedUrlInfo urlInfo = s3Service.generatePresignedUrl(contentType);
        PresignedUrlResponse response = PresignedUrlResponse.from(urlInfo);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Presigned URL 발급 성공", response));
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
        List<String> allowedTypes = List.of(
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
        );

        if (!allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "허용되지 않는 파일 형식입니다: " + contentType +
                    ". 허용 형식: " + String.join(", ", allowedTypes)
            );
        }
    }
}
