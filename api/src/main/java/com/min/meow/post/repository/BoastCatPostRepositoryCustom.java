package com.min.meow.post.repository;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BoastCatPostRepositoryCustom {

    // 검색 메서드: title, contents, userId 필터 (Projection 적용)
    Page<BoastCatPostListResponse> search(String title, String contents, Long userId, Pageable pageable);

    /**
     * 최근 게시물 20개 Projection 조회
     * - DB에서 필요한 컬럼만 SELECT하여 성능 최적화
     * - Entity 변환 없이 DTO로 직접 조회
     * - BoastCatPostListResponse를 재사용하여 코드 중복 제거
     */
    List<BoastCatPostListResponse> findTop20RecentWithProjection();

    /**
     * 게시글 목록 페이징 조회 (Projection 적용)
     * - 목록에서 필요한 필드만 SELECT (title, writer, likeCount, commentCount, view, createdAt)
     * - contents, imageUrls, comments 등 불필요한 데이터 제외
     * - Entity 변환 없이 DTO로 직접 매핑
     * @param pageable 페이징 정보
     * @return 경량화된 목록 응답 DTO 페이지
     */
    Page<BoastCatPostListResponse> findAllWithProjection(Pageable pageable);

}