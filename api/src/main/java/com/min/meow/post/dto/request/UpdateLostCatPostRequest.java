package com.min.meow.post.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 실종 고양이 게시글 수정 요청 DTO
 * Presigned URL 기반 이미지 업로드 방식으로 변경:
 * - 새 이미지: Presigned URL로 S3에 업로드 후 key 전달
 * - 유지할 이미지: 기존 CloudFront URL 그대로 전달
 * - 삭제할 이미지: CloudFront URL 전달 (서버에서 S3 key 추출 후 삭제)
 */
@Schema(description = "실종글 수정 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLostCatPostRequest {

    @Schema(description = "게시글 제목 (2~100자)", example = "수정된 실종 신고 제목")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;

    @Schema(description = "게시글 본문 (최대 2000자)", example = "수정된 실종 신고 내용")
    @Size(max = 2000, message = "내용은 2000자 이하로 입력해주세요.")
    private String content;

    @Schema(description = "고양이 이름 (최대 20자)", example = "나비")
    @Size(max = 20, message = "고양이 이름은 20자 이하로 입력해주세요.")
    private String catName;

    @Schema(description = "고양이 품종 (최대 30자)", example = "코리안숏헤어")
    @Size(max = 30, message = "품종은 30자 이하로 입력해주세요.")
    private String catType;

    @Schema(description = "고양이 색상 (최대 20자)", example = "치즈색")
    @Size(max = 20, message = "색상은 20자 이하로 입력해주세요.")
    private String catColor;

    @Schema(description = "고양이 나이 (0~30살)", example = "3")
    @Min(value = 0, message = "나이는 0 이상이어야 합니다.")
    @Max(value = 30, message = "나이는 30 이하로 입력해주세요.")
    private Integer catAge;

    @Schema(description = "고양이 몸무게 (0~30kg)", example = "4")
    @Min(value = 0, message = "몸무게는 0 이상이어야 합니다.")
    @Max(value = 30, message = "몸무게는 30kg 이하로 입력해주세요.")
    private Integer catWeight;

    @Schema(description = "고양이 성별 (MALE/FEMALE/UNKNOWN)", example = "MALE")
    @Size(max = 10, message = "성별은 10자 이하로 입력해주세요.")
    private String catGender;

    @Schema(description = "실종일자", example = "2025-01-15")
    private LocalDate lostDate;

    @Schema(description = "실종 장소 (최대 100자)", example = "서울시 강남구 역삼동")
    @Size(max = 100, message = "실종 장소는 100자 이하로 입력해주세요.")
    private String lostLocation;

    // 수정은 부분 업데이트라 nullable 유지(둘 다 null이면 위치 변경 안 함)
    // 다만 한쪽만 변경되는 케이스는 isCoordinatesValid()에서 차단
    @Schema(description = "실종 장소 위도 (-90 ~ 90)", example = "37.4979")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다.")
    private Double latitude;

    @Schema(description = "실종 장소 경도 (-180 ~ 180)", example = "127.0276")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하이어야 합니다.")
    private Double longitude;

    @Schema(description = "사례금 (0원 이상)", example = "50000")
    @Min(value = 0, message = "사례금은 0원 이상이어야 합니다.")
    private Integer reward;

    @Schema(description = "고양이 발견 완료 여부", example = "false")
    private boolean isCompleted;

    @Schema(description = "새로 업로드한 이미지의 S3 key 목록 (최대 10장)",
            example = "[\"meow/uuid-new-1.jpg\"]")
    @Size(max = 10, message = "이미지는 최대 10장까지 업로드 가능합니다.")
    private List<String> newImageKeys;

    @Schema(description = "유지할 기존 이미지의 CloudFront URL 목록",
            example = "[\"https://d1234.cloudfront.net/meow/uuid-old.jpg\"]")
    private List<String> keepImageUrls;

    @Schema(description = "삭제할 이미지의 CloudFront URL 목록",
            example = "[\"https://d1234.cloudfront.net/meow/uuid-delete.jpg\"]")
    private List<String> deleteImageUrls;

    // 필드 간 관계 검증: 위도와 경도는 모두 있거나 모두 없어야 함
    @AssertTrue(message = "위도와 경도는 모두 입력하거나, 모두 비워야 합니다.")
    @Schema(hidden = true)
    private boolean isCoordinatesValid() {
        return (latitude == null) == (longitude == null);
    }

    // 필드 간 관계 검증: 유지할 이미지와 삭제할 이미지에 중복 URL이 없어야 함
    @AssertTrue(message = "유지할 이미지와 삭제할 이미지에 중복된 URL이 있습니다.")
    @Schema(hidden = true)
    private boolean isImageUrlsNotOverlapping() {
        if (keepImageUrls == null || deleteImageUrls == null) return true;
        return keepImageUrls.stream().noneMatch(deleteImageUrls::contains);
    }

    // 제목: 앞뒤 공백 제거
    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    // 본문: 줄바꿈/들여쓰기가 의도적일 수 있으므로 정규화 안 함
    public void setContent(String content) {
        this.content = content;
    }

    // 고양이 이름: 앞뒤 공백 제거 (검색 정확도 향상)
    public void setCatName(String catName) {
        this.catName = catName != null ? catName.trim() : null;
    }

    // 고양이 품종: 앞뒤 공백 제거
    public void setCatType(String catType) {
        this.catType = catType != null ? catType.trim() : null;
    }

    // 고양이 색상: 앞뒤 공백 제거
    public void setCatColor(String catColor) {
        this.catColor = catColor != null ? catColor.trim() : null;
    }

    // 실종 장소: 앞뒤 공백 제거
    public void setLostLocation(String lostLocation) {
        this.lostLocation = lostLocation != null ? lostLocation.trim() : null;
    }

    // 숫자/boolean/리스트: 정규화 불필요
    public void setCatAge(Integer catAge) {
        this.catAge = catAge;
    }

    public void setCatWeight(Integer catWeight) {
        this.catWeight = catWeight;
    }

    public void setCatGender(String catGender) {
        this.catGender = catGender;
    }

    public void setLostDate(LocalDate lostDate) {
        this.lostDate = lostDate;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setReward(Integer reward) {
        this.reward = reward;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public void setNewImageKeys(List<String> newImageKeys) {
        this.newImageKeys = newImageKeys;
    }

    public void setKeepImageUrls(List<String> keepImageUrls) {
        this.keepImageUrls = keepImageUrls;
    }

    public void setDeleteImageUrls(List<String> deleteImageUrls) {
        this.deleteImageUrls = deleteImageUrls;
    }
}
