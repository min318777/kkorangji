package com.min.meow.post.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * 고양이 자랑글 수정 요청 DTO
 *
 * Presigned URL 기반 이미지 업로드 방식으로 변경:
 * - 새 이미지: Presigned URL로 S3에 업로드 후 key 전달
 * - 유지할 이미지: 기존 CloudFront URL 그대로 전달
 * - 삭제할 이미지: CloudFront URL 전달 (서버에서 S3 key 추출 후 삭제)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBoastCatPostRequest {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;

    @Size(max = 1000, message = "1000자 이하로 작성해주세요.")
    private String content;

    /**
     * 새로 업로드한 이미지의 S3 key 목록
     * Presigned URL로 업로드 완료 후 받은 key를 전달
     * 예: ["meow/uuid-new-1.jpg"]
     */
    private List<String> newImageKeys;

    /**
     * 유지할 기존 이미지 URL 목록
     * 기존 CloudFront URL 그대로 전달
     * 예: ["https://d1234.cloudfront.net/meow/uuid-old.jpg"]
     */
    private List<String> keepImageUrls;

    /**
     * 삭제할 이미지 URL 목록
     * 기존 CloudFront URL 전달 (서버에서 S3 key 추출 후 삭제)
     * 예: ["https://d1234.cloudfront.net/meow/uuid-delete.jpg"]
     */
    private List<String> deleteImageUrls;
}
