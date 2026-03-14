package com.min.meow.config;

import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * S3 서비스
 *
 * Presigned URL 기반 이미지 업로드 아키텍처:
 * 1. 클라이언트가 서버에 Presigned URL 요청
 * 2. 서버가 S3 Presigned URL 생성하여 반환
 * 3. 클라이언트가 Presigned URL로 S3에 직접 업로드 (서버 트래픽 없음!)
 * 4. 업로드 완료 후 클라이언트가 S3 key를 서버에 전달
 * 5. 서버가 key를 CloudFront URL로 변환하여 DB 저장
 *
 * 장점:
 * - 서버 트래픽 비용 절감 (이미지가 서버를 거치지 않음)
 * - 서버 메모리 부담 감소
 * - CloudFront CDN으로 빠른 이미지 로딩 + 캐싱
 * - S3 직접 접근 차단으로 보안 강화
 */
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

    /**
     * 업로드용 Presigned URL 생성
     *
     * @param contentType 업로드할 파일의 Content-Type (예: image/jpeg)
     * @return Presigned URL 정보 (url, key)
     */
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

    /**
     * 여러 개의 Presigned URL 생성
     *
     * @param contentTypes Content-Type 목록
     * @return Presigned URL 정보 목록
     */
    public List<PresignedUrlInfo> generatePresignedUrls(List<String> contentTypes) {
        return contentTypes.stream()
                .map(this::generatePresignedUrl)
                .collect(Collectors.toList());
    }

    /**
     * S3 key를 CloudFront URL로 변환
     *
     * @param key S3 object key (예: meow/uuid-xxx.jpg)
     * @return CloudFront URL (예: https://d1234.cloudfront.net/meow/uuid-xxx.jpg)
     */
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

    /**
     * 여러 S3 key를 CloudFront URL 목록으로 변환
     *
     * @param keys S3 key 목록
     * @return CloudFront URL 목록
     */
    public List<String> toCloudFrontUrls(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream()
                .map(this::toCloudFrontUrl)
                .collect(Collectors.toList());
    }

    /**
     * S3 파일 존재 여부 확인
     *
     * @param key S3 object key
     * @return 존재 여부
     */
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

    /**
     * S3 파일 삭제
     *
     * @param key S3 object key
     */
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

    /**
     * 여러 S3 파일 삭제
     *
     * @param keys S3 key 목록
     */
    public void deleteFiles(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        keys.forEach(this::deleteFile);
    }

    /**
     * CloudFront URL에서 S3 key 추출
     *
     * @param cloudFrontUrl CloudFront URL
     * @return S3 key
     */
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

    /**
     * Content-Type에서 파일 확장자 추출
     */
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
            default -> "";
        };
    }

    /**
     * Presigned URL 정보를 담는 내부 클래스
     */
    public record PresignedUrlInfo(
            String presignedUrl,  // S3 Presigned PUT URL
            String key            // S3 object key
    ) {}
}
