package com.min.meow.user.dto.reponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 내가 쓴 댓글 목록 응답 DTO
 * 페이징 정보 포함
 */
@Schema(description = "내가 쓴 댓글 목록 응답")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyCommentListResponse {

    @Schema(description = "댓글 목록")
    private List<MyCommentDto> content;

    @Schema(description = "전체 댓글 수", example = "30")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "3")
    private int totalPages;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int currentPage;

    @Schema(description = "페이지 크기", example = "10")
    private int size;

    /**
     * Page<MyCommentDto>를 Response로 변환
     */
    public static MyCommentListResponse from(Page<MyCommentDto> page) {
        return MyCommentListResponse.builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
