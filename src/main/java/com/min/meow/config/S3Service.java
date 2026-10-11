package com.min.meow.config;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// S3 서비스 — 클라이언트가 Presigned URL로 S3에 직접 업로드하고, 서버는 key를 CloudFront URL로 변환해 저장
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    // Presigned URL 유효 시간 (10분)
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(10);

    // S3 저장 경로 prefix
    private static final String KEY_PREFIX = "meow/";

    // 업로드용 Presigned URL 생성
    public PresignedUrlInfo generatePresignedUrl(String contentType) {
        // 고유한 S3 key 생성 (UUID + 확장자)
        String extension = getExtensionFromContentType(contentType);
        String key = KEY_PREFIX + UUID.randomUUID() + extension;

        // Presigned PUT URL 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_EXPIRATION)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedRequest.url().toString();

        log.info("Presigned URL 생성 완료 - key: {}, 만료: {}분", key, PRESIGNED_URL_EXPIRATION.toMinutes());

        return new PresignedUrlInfo(presignedUrl, key);
    }

    // 여러 개의 Presigned URL 생성
    public List<PresignedUrlInfo> generatePresignedUrls(List<String> contentTypes) {
        return contentTypes.stream()
                .map(this::generatePresignedUrl)
                .collect(Collectors.toList());
    }

    // S3 key를 CloudFront URL로 변환
    public String toCloudFrontUrl(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        // cloudfrontDomain이 /로 끝나면 제거
        String domain = cloudfrontDomain.endsWith("/")
                ? cloudfrontDomain.substring(0, cloudfrontDomain.length() - 1)
                : cloudfrontDomain;
        return domain + "/" + key;
    }

    // 여러 S3 key를 CloudFront URL 목록으로 변환
    public List<String> toCloudFrontUrls(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(this::toCloudFrontUrl)
                .collect(Collectors.toList());
    }

    // S3 파일 존재 여부 확인
    public boolean doesObjectExist(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    // S3 파일 삭제
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("S3 파일 삭제 완료 - key: {}", key);
        } catch (SdkException e) {
            // 예외를 삼키지 않고 래핑해서 전파 (FastAPI의 raise DatabaseException() from e 패턴)
            // 원인(cause) 체인을 보존하여 디버깅 시 근본 원인 추적 가능
            log.error("S3 파일 삭제 실패 - key: {}", key, e);
            throw new CustomException(ErrorCode.S3_DELETE_FAILED, e);
        }
    }

    // 여러 S3 파일 삭제 — 일부 실패해도 나머지 계속 진행, 실패 목록은 모두 시도 후 한 번에 예외로 전파
    public void deleteFiles(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        List<String> failedKeys = new ArrayList<>();
        for (String key : keys) {
            try {
                deleteFile(key);
            } catch (CustomException e) {
                // 실패해도 나머지 파일 삭제 계속 진행
                failedKeys.add(key);
                log.warn("S3 파일 삭제 실패, 계속 진행 - key: {}", key);
            }
        }

        if (!failedKeys.isEmpty()) {
            log.error("S3 파일 일부 삭제 실패 - 실패 수: {}, keys: {}", failedKeys.size(), failedKeys);
            throw new CustomException(ErrorCode.S3_DELETE_FAILED,
                    "삭제 실패한 파일 수: " + failedKeys.size());
        }
    }

    // CloudFront URL에서 S3 key 추출
    public String extractKeyFromUrl(String cloudFrontUrl) {
        if (cloudFrontUrl == null || cloudFrontUrl.isEmpty()) {
            return null;
        }
        // https://d1234.cloudfront.net/meow/uuid-xxx.jpg -> meow/uuid-xxx.jpg
        String domain = cloudfrontDomain.endsWith("/")
                ? cloudfrontDomain.substring(0, cloudfrontDomain.length() - 1)
                : cloudfrontDomain;
        if (cloudFrontUrl.startsWith(domain)) {
            return cloudFrontUrl.substring(domain.length() + 1); // +1 for "/"
        }
        // 이미 key만 있는 경우
        return cloudFrontUrl;
    }

    // Content-Type에서 파일 확장자 추출
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            case "video/mp4" -> ".mp4";
            default -> "";
        };
    }

    // Presigned URL 정보를 담는 내부 클래스
    public record PresignedUrlInfo(
            String presignedUrl,  // S3 Presigned PUT URL
            String key            // S3 object key
    ) {}
}
