package com.min.meow.post.repository;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BoastCatPostRepositoryCustom {

    // LIKE 검색: '%keyword%' 방식 (성능 비교 기준선)
    Page<BoastCatPostListResponse> search(String title, String contents, Long userId, Pageable pageable);

    // Full-Text Search (ngram): MATCH(title, contents) AGAINST(keyword IN BOOLEAN MODE)
    Page<BoastCatPostListResponse> searchByKeyword(String keyword, Long userId, Pageable pageable);

    /**
     * 게시글 목록 페이징 조회 (Projection 적용)
     */
    Page<BoastCatPostListResponse> findAllWithProjection(Pageable pageable);

}
