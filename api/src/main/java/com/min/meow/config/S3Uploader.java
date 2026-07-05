package com.min.meow.config;


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

/**
 * @deprecated Presigned URL 기반 이미지 업로드 방식으로 전환되어 더 이상 사용되지 않음.
 * 대신 {@link S3Service}를 사용하세요.
 * <p>기존 방식의 문제점:</p>
 * <ul>
 *   <li>이미지가 서버를 거쳐 S3로 업로드되어 서버 리소스(메모리, 대역폭) 소모</li>
 *   <li>서버 트래픽 비용 증가</li>
 *   <li>대용량 이미지 처리 시 서버 부하 발생</li>
 * </ul>
 * <p>새로운 방식 (S3Service + Presigned URL):</p>
 * <ul>
 *   <li>클라이언트가 S3에 직접 업로드하여 서버 리소스 절약</li>
 *   <li>CloudFront CDN을 통한 이미지 조회로 응답 속도 향상</li>
 *   <li>서버 트래픽 비용 절감</li>
 * </ul>
 * @see S3Service
 */
@Deprecated(since = "2025.02", forRemoval = true)
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.baseUrl}")
    private String baseUrl;

    /**
     * @deprecated Presigned URL 기반 방식으로 전환됨
     * @see S3Service#toCloudFrontUrls(List)
     */
    @Deprecated
    public List<String> uploadFiles(List<MultipartFile> files){
        return files.stream()
                .map(this::uploadFile)
                .collect(Collectors.toList());
    }

    /**
     * @deprecated Presigned URL 기반 방식으로 전환됨
     * @see S3Service#generatePresignedUrl(String)
     */
    @Deprecated
    public String uploadFile(MultipartFile file) {
        String key = "meow/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
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
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }
}
