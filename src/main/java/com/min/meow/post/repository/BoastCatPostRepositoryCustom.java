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

    // Full-Text Search (ngram): MATCH(title, contents) AGAINST(keyword IN NATURAL LANGUAGE MODE)
    Page<BoastCatPostListResponse> searchByNaturalLanguage(String keyword, Long userId, Pageable pageable);

    Page<BoastCatPostListResponse> findAllWithProjection(Pageable pageable);

    // count 캐싱용 - content만 조회 (COUNT 쿼리 없음)
    List<BoastCatPostListResponse> findContentWithProjection(Pageable pageable);

    // count 캐싱용 - 전체 게시글 수만 조회
    long countAllPosts();

    // 커버링 인덱스 서브쿼리 + JOIN 방식 (OFFSET 대용량 성능 최적화)
    List<BoastCatPostListResponse> findContentWithCoveringIndex(Pageable pageable);

    // 커버링 인덱스 서브쿼리 + IN 절 방식 (JOIN 방식과 성능 비교용)
    List<BoastCatPostListResponse> findContentWithCoveringIndexUsingIn(Pageable pageable);

}
