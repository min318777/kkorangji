package com.min.meow.image.dto.response;

import com.min.meow.config.S3Service;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 응답 DTO
 *
 * 클라이언트는 이 정보를 사용하여:
 * 1. presignedUrl로 이미지를 S3에 직접 업로드 (PUT 요청)
 * 2. 업로드 완료 후 key를 게시글 생성 API에 전달
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {

    /**
     * S3 Presigned PUT URL
     * 클라이언트가 이 URL로 PUT 요청하여 이미지를 직접 업로드
     * 예: https://bucket.s3.ap-northeast-2.amazonaws.com/meow/uuid-xxx.jpg?X-Amz-...
     */
    private String presignedUrl;

    /**
     * S3 object key
     * 업로드 완료 후 게시글 생성/수정 시 이 key를 전달
     * 예: meow/uuid-xxx.jpg
     */
    private String key;

    /**
     * S3Service.PresignedUrlInfo에서 변환
     */
    public static PresignedUrlResponse from(S3Service.PresignedUrlInfo info) {
        return PresignedUrlResponse.builder()
                .presignedUrl(info.presignedUrl())
                .key(info.key())
                .build();
    }
}
