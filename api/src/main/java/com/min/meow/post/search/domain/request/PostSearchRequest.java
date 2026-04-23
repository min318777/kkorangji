package com.min.meow.post.search.domain.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "게시글 검색 요청")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostSearchRequest {

    @Schema(description = "제목 검색어 (부분 일치)", example = "고양이")
    private String title;

    @Schema(description = "본문 검색어 (부분 일치)", example = "귀여운")
    private String contents;

    @Schema(description = "작성자 ID (특정 유저 글만 검색)", example = "1")
    private Long userId;

    // 검색어: 앞뒤 공백 제거 (검색 정확도 향상)
    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    // 검색어: 앞뒤 공백 제거
    public void setContents(String contents) {
        this.contents = contents != null ? contents.trim() : null;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
