package com.min.meow.post.repository;

import com.min.meow.post.dto.response.LostCatPostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 실종 고양이 게시글 커스텀 리포지토리 인터페이스
 *
 * QueryDSL을 사용한 DTO Projection 메서드 정의
 * - 성능 최적화: 필요한 컬럼만 SELECT
 * - LazyInitializationException 방지: Entity 대신 DTO 직접 조회
 */
public interface LostCatRepositoryCustom {

    /**
     * 게시글 목록 페이징 조회 (Projection 적용)
     *
     * - 목록에서 필요한 필드만 SELECT
     * - contents, imageUrls, comments 등 불필요한 데이터 제외
     * - Entity 변환 없이 DTO로 직접 매핑
     *
     * @param pageable 페이징 정보
     * @return 경량화된 목록 응답 DTO 페이지
     */
    Page<LostCatPostListResponse> findAllWithProjection(Pageable pageable);

    /**
     * 최근 게시물 20개 Projection 조회
     *
     * - 메인페이지 최근글 목록용
     * - DB에서 필요한 컬럼만 SELECT하여 성능 최적화
     * - Entity 변환 없이 DTO로 직접 조회
     * - LostCatPostListResponse를 재사용하여 코드 중복 제거
     *
     * @return 최근 게시글 20개 DTO 리스트
     */
    List<LostCatPostListResponse> findTop20RecentWithProjection();
}
