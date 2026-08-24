package com.min.meow.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Schema(description = "게시글 FTS 검색 요청 (Full-Text Search, ngram)")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostSearchRequest {

    @Schema(description = "검색어 (제목+내용 통합, 2글자 이상)", example = "고양이")
    @NotBlank(message = "검색어는 필수입니다.")
    @Size(min = 2, message = "검색어는 2글자 이상이어야 합니다.")
    private String keyword;

    @Schema(description = "작성자 ID (특정 유저 글만 검색)", example = "1")
    private Long userId;

    // 검색어 앞뒤 공백 제거
    public void setKeyword(String keyword) {
        this.keyword = keyword != null ? keyword.trim() : null;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
