package com.min.meow.post.search.domain.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "게시글 검색 요청")
@Getter
@Setter
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
}
