package com.min.meow.post.search.domain.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "게시글 검색 요청")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostSearchRequest {

    @Schema(description = "게시글 ID (정확히 일치)", example = "1")
    private Long id;

    @Schema(description = "제목 검색어 (부분 일치)", example = "고양이")
    private String title;

    @Schema(description = "본문 검색어 (부분 일치)", example = "귀여운")
    private String contents;

    @Schema(description = "최소 조회수 필터", example = "0")
    private int view;

    @Schema(description = "작성자 ID", example = "1")
    private Long userId;
    //private int CategoryType;
    //private CategoryDto.SortStatus sortStatus;

    // 검색어: 앞뒤 공백 제거 (검색 정확도 향상)
    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    // 검색어: 앞뒤 공백 제거
    public void setContents(String contents) {
        this.contents = contents != null ? contents.trim() : null;
    }

    // 숫자: 정규화 불필요
    public void setId(Long id) {
        this.id = id;
    }

    public void setView(int view) {
        this.view = view;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
