package com.min.meow.post.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * 고양이 자랑글 수정 요청 DTO
 * Presigned URL 기반 이미지 업로드 방식:
 * - images 리스트 하나로 최종 이미지 순서를 그대로 전달 (기존 이미지 + 새 이미지 혼합 가능)
 * - EXISTING 타입: value = 기존 CloudFront URL
 * - NEW 타입: value = 새로 업로드한 S3 key
 * - images에 포함되지 않은 기존 이미지는 자동으로 삭제 처리됨
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

    @Schema(description = "최종 이미지 목록 (순서 = 노출 순서, 기존/신규 이미지 혼합 가능, 최대 10장)",
            example = "[{\"type\": \"EXISTING\", \"value\": \"https://d1234.cloudfront.net/meow/uuid-old.jpg\"}, {\"type\": \"NEW\", \"value\": \"meow/uuid-new-1.jpg\"}]")
    @Size(max = 10, message = "이미지는 최대 10장까지 업로드 가능합니다.")
    private List<ImageItemRequest> images;

    // 제목: 앞뒤 공백 제거
    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    // 본문: 줄바꿈/들여쓰기가 의도적일 수 있으므로 정규화 안 함
    public void setContent(String content) {
        this.content = content;
    }

    // 리스트: 정규화 불필요
    public void setImages(List<ImageItemRequest> images) {
        this.images = images;
    }
}
