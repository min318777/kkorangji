package com.min.meow.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Schema(description = "게시글 LIKE 검색 요청 (성능 비교용, '%keyword%' 방식)")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostLikeSearchRequest {

    @Schema(description = "검색어 (제목+내용 통합, 2글자 이상)", example = "고양이")
    @NotBlank(message = "검색어는 필수입니다.")
    @Size(min = 2, message = "검색어는 2글자 이상이어야 합니다.")
    private String keyword;

    // 검색어 앞뒤 공백 제거
    public void setKeyword(String keyword) {
        this.keyword = keyword != null ? keyword.trim() : null;
    }
}
