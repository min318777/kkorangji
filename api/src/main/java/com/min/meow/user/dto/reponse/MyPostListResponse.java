package com.min.meow.user.dto.reponse;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 내가 쓴 게시글 목록 응답 DTO
 * 페이징 정보 포함
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyPostListResponse {

    private List<MyPostDto> content;  // 게시글 목록
    private long totalElements;       // 전체 게시글 수
    private int totalPages;           // 전체 페이지 수
    private int currentPage;          // 현재 페이지 번호
    private int size;                 // 페이지 크기

    /**
     * Page<MyPostDto>를 Response로 변환
     */
    public static MyPostListResponse from(Page<MyPostDto> page) {
        return MyPostListResponse.builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .build();
    }
}