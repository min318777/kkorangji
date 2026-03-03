package com.min.meow.post.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * 고양이 자랑글 수정 요청 DTO
 * Presigned URL 기반 이미지 업로드 방식으로 변경:
 * - 새 이미지: Presigned URL로 S3에 업로드 후 key 전달
 * - 유지할 이미지: 기존 CloudFront URL 그대로 전달
 * - 삭제할 이미지: CloudFront URL 전달 (서버에서 S3 key 추출 후 삭제)
 */
@Schema(description = "자랑글 수정 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBoastCatPostRequest {

    // 제목: 2~100자, 선택 (null이면 기존 값 유지 - PATCH 스타일)
    @Schema(description = "게시글 제목 (2~100자)", example = "수정된 제목입니다")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;

    // 본문: 최대 2000자, 선택
    @Schema(description = "게시글 본문 (최대 2000자, 선택)", example = "수정된 내용입니다")
    @Size(max = 2000, message = "내용은 2000자 이하로 입력해주세요.")
    private String content;

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

    // 리스트: 정규화 불필요
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
