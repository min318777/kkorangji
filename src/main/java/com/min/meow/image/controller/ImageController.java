package com.min.meow.image.controller;

import com.min.meow.config.S3Service;
import com.min.meow.common.ApiResponse;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.image.dto.request.PresignedFileRequest;
import com.min.meow.image.dto.request.PresignedUrlRequest;
import com.min.meow.image.dto.response.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "이미지", description = "S3 Presigned URL 발급 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final S3Service s3Service;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    private static final String ALLOWED_VIDEO_TYPE = "video/mp4";

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;   // 10MB
    private static final long MAX_VIDEO_BYTES = 200L * 1024 * 1024;  // 200MB

    private static final int MAX_IMAGE_COUNT = 10;
    private static final int MAX_VIDEO_COUNT = 1;

    @Operation(summary = "Presigned URL 여러개 발급",
            description = "업로드할 파일(Content-Type, 크기)만큼 S3 Presigned URL을 발급합니다. "
                    + "이미지 허용 형식: image/jpeg, image/jpg, image/png, image/gif, image/webp (최대 10장, 장당 10MB). "
                    + "동영상 허용 형식: video/mp4 (최대 1개, 200MB). 인증 필요.")
    @PostMapping("/presigned-urls")
    public ResponseEntity<ApiResponse<List<PresignedUrlResponse>>> getPresignedUrls(
            @Valid @RequestBody PresignedUrlRequest request) {

        log.info("Presigned URL 요청 - 개수: {}", request.getFiles().size());

        validateFiles(request.getFiles());

        List<PresignedUrlResponse> responses = request.getFiles().stream()
                .map(f -> PresignedUrlResponse.from(s3Service.generatePresignedUrl(f.getContentType())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Presigned URL 발급 성공", responses));
    }

    @Operation(summary = "Presigned URL 단건 발급",
            description = "단일 파일 업로드용 Presigned URL을 발급합니다. "
                    + "이미지 허용 형식: image/jpeg, image/jpg, image/png, image/gif, image/webp (최대 10MB). "
                    + "동영상 허용 형식: video/mp4 (최대 200MB). 인증 필요. "
                    + "Body: { \"contentType\": \"image/jpeg\", \"fileSize\": 3145728 }")
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @Valid @RequestBody PresignedFileRequest request) {

        log.info("단일 Presigned URL 요청 - contentType: {}, fileSize: {}", request.getContentType(), request.getFileSize());

        validateFile(request);

        S3Service.PresignedUrlInfo urlInfo = s3Service.generatePresignedUrl(request.getContentType());
        PresignedUrlResponse response = PresignedUrlResponse.from(urlInfo);

        return ResponseEntity.ok(ApiResponse.success("Presigned URL 발급 성공", response));
    }

    /**
     * 파일 목록 검증 — 타입/크기(개별) + 이미지/동영상 개수(전체)
     */
    private void validateFiles(List<PresignedFileRequest> files) {
        long imageCount = files.stream().filter(f -> isImageType(f.getContentType())).count();
        long videoCount = files.stream().filter(f -> isVideoType(f.getContentType())).count();

        if (imageCount > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.IMAGE_COUNT_EXCEEDED, "이미지 개수: " + imageCount);
        }
        if (videoCount > MAX_VIDEO_COUNT) {
            throw new CustomException(ErrorCode.IMAGE_COUNT_EXCEEDED, "동영상 개수: " + videoCount);
        }

        for (PresignedFileRequest file : files) {
            validateFile(file);
        }
    }

    /**
     * 파일 1개 검증 — Content-Type 허용 여부 + 크기 제한
     * 크기 검증은 클라이언트가 신고한 fileSize를 서버가 신뢰하여 비교하는 방식.
     */
    private void validateFile(PresignedFileRequest file) {
        String contentType = file.getContentType();

        if (isImageType(contentType)) {
            if (file.getFileSize() > MAX_IMAGE_BYTES) {
                throw new CustomException(ErrorCode.IMAGE_SIZE_EXCEEDED, "요청된 크기: " + file.getFileSize());
            }
        } else if (isVideoType(contentType)) {
            if (file.getFileSize() > MAX_VIDEO_BYTES) {
                throw new CustomException(ErrorCode.IMAGE_SIZE_EXCEEDED, "요청된 크기: " + file.getFileSize());
            }
        } else {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FORMAT, "요청된 형식: " + contentType);
        }
    }

    private boolean isImageType(String contentType) {
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
    }

    private boolean isVideoType(String contentType) {
        return contentType != null && ALLOWED_VIDEO_TYPE.equalsIgnoreCase(contentType);
    }
}
