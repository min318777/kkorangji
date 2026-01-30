package com.min.meow.post.repository;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.RecentBoastCatPostResponse;
import com.min.meow.post.entity.BoastCatPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BoastCatPostRepositoryCustom {

    // 검색 메서드: title과 contents를 직접 파라미터로 받음
    Page<BoastCatPost> search(String title, String contents, Pageable pageable);

    /**
     * 최근 게시물 20개 Projection 조회
     * - DB에서 필요한 컬럼만 SELECT하여 성능 최적화
     * - Entity 변환 없이 DTO로 직접 조회
     */
    List<RecentBoastCatPostResponse> findTop20RecentWithProjection();

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