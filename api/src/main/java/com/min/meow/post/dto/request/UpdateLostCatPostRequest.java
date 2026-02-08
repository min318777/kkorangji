package com.min.meow.post.dto.request;


import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * 실종 고양이 게시글 수정 요청 DTO
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
public class UpdateLostCatPostRequest {

    @Size(min = 2, max = 100, message = "제목은 2자이상 100자이하로 입력해주세요.")
    private String title;
    private String content;
    private String catName;
    private String catType;
    private String catColor;
    private Integer catAge;
    private Integer catWeight;
    private String lostLocation;
    private Double latitude;
    private Double longitude;
    private Integer reward;
    private boolean isCompleted;

    /**
     * 새로 업로드한 이미지의 S3 key 목록
     * Presigned URL로 업로드 완료 후 받은 key를 전달
     */
    private List<String> newImageKeys;

    /**
     * 유지할 기존 이미지 URL 목록
     * 기존 CloudFront URL 그대로 전달
     */
    private List<String> keepImageUrls;

    /**
     * 삭제할 이미지 URL 목록
     * 기존 CloudFront URL 전달 (서버에서 S3 key 추출 후 삭제)
     */
    private List<String> deleteImageUrls;
}
