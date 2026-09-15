package com.min.meow.post.repository;

import com.min.meow.post.dto.response.LostCatPostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LostCatRepositoryCustom {

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

    // count 캐싱용 - content만 조회 (COUNT 쿼리 없음)
    List<LostCatPostListResponse> findContentWithProjection(Pageable pageable);

    // count 캐싱용 - 전체 게시글 수만 조회
    long countAllPosts();

    // 커버링 인덱스 서브쿼리 + IN 절 방식 (OFFSET 대용량 성능 최적화)
    List<LostCatPostListResponse> findContentWithCoveringIndexUsingIn(Pageable pageable);

}
