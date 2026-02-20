package com.min.meow.post.dto.request;


import jakarta.validation.constraints.*;
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

    // 제목: 2~100자
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;

    // 본문: 최대 2000자
    @Size(max = 2000, message = "내용은 2000자 이하로 입력해주세요.")
    private String content;

    // 고양이 이름: 최대 20자
    @Size(max = 20, message = "고양이 이름은 20자 이하로 입력해주세요.")
    private String catName;

    // 고양이 품종: 최대 30자
    @Size(max = 30, message = "품종은 30자 이하로 입력해주세요.")
    private String catType;

    // 고양이 색상: 최대 20자
    @Size(max = 20, message = "색상은 20자 이하로 입력해주세요.")
    private String catColor;

    // 고양이 나이: 0~30살
    @Min(value = 0, message = "나이는 0 이상이어야 합니다.")
    @Max(value = 30, message = "나이는 30 이하로 입력해주세요.")
    private Integer catAge;

    // 고양이 몸무게: 0~30kg
    @Min(value = 0, message = "몸무게는 0 이상이어야 합니다.")
    @Max(value = 30, message = "몸무게는 30kg 이하로 입력해주세요.")
    private Integer catWeight;

    // 실종 장소: 최대 100자
    @Size(max = 100, message = "실종 장소는 100자 이하로 입력해주세요.")
    private String lostLocation;

    private Double latitude;
    private Double longitude;

    // 사례금: 0원 이상
    @Min(value = 0, message = "사례금은 0원 이상이어야 합니다.")
    private Integer reward;

    private boolean isCompleted;

    /**
     * 새로 업로드한 이미지의 S3 key 목록
     * Presigned URL로 업로드 완료 후 받은 key를 전달
     */
    @Size(max = 10, message = "이미지는 최대 10장까지 업로드 가능합니다.")
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