package com.min.meow.post.repository;

import com.min.meow.post.dto.response.LostCatPostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 실종 고양이 게시글 커스텀 리포지토리 인터페이스
 * QueryDSL을 사용한 DTO Projection 메서드 정의
 * - 성능 최적화: 필요한 컬럼만 SELECT
 * - LazyInitializationException 방지: Entity 대신 DTO 직접 조회
 */
public interface LostCatRepositoryCustom {

    /**
     * 게시글 목록 페이징 조회 (Projection 적용)
     * - 목록에서 필요한 필드만 SELECT
     * - contents, imageUrls, comments 등 불필요한 데이터 제외
     * - Entity 변환 없이 DTO로 직접 매핑
     * @param pageable 페이징 정보
     * @return 경량화된 목록 응답 DTO 페이지
     */
    Page<LostCatPostListResponse> findAllWithProjection(Pageable pageable);

    /**
     * 내 주변 실종글 페이징 조회 (Bounding Box 방식)
     * 위도/경도 범위로 대략적인 주변 게시글을 필터링합니다.
     * latitude/longitude가 null인 게시글은 제외됩니다.
     * @param lat       현재 위치 위도
     * @param lng       현재 위치 경도
     * @param radiusKm  검색 반경 (km)
     * @param pageable  페이징 정보
     */
    Page<LostCatPostListResponse> findNearbyWithProjection(double lat, double lng, double radiusKm, Pageable pageable);

    // 내 주변 실종글 조회 (ST_Distance_Sphere 방식) — 정확한 원형 반경 + 거리순 정렬
    Page<LostCatPostListResponse> findNearbyWithST(double lat, double lng, double radiusKm, Pageable pageable);

    // LIKE 검색: '%keyword%' 방식
    Page<LostCatPostListResponse> search(String title, String contents, Long userId, Pageable pageable);

    // FTS 검색: MATCH AGAINST (ngram)
    Page<LostCatPostListResponse> searchByKeyword(String keyword, Long userId, Pageable pageable);

}
