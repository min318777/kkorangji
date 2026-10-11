package com.min.meow.config;


import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// deprecated: 서버 경유 업로드 방식(메모리/트래픽 부담) → Presigned URL 기반 S3Service로 전환됨
@Deprecated(since = "2025.02", forRemoval = true)
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.baseUrl}")
    private String baseUrl;

    // deprecated: S3Service#toCloudFrontUrls로 대체됨
    @Deprecated
    public List<String> uploadFiles(List<MultipartFile> files){
        return files.stream()
                .map(this::uploadFile)
                .collect(Collectors.toList());
    }

    // deprecated: S3Service#generatePresignedUrl로 대체됨
    @Deprecated
    public String uploadFile(MultipartFile file) {
        String key = "kkorangji/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromBytes(file.getBytes()));
            return baseUrl + "/" + key;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED, e);
        }
    }
}
