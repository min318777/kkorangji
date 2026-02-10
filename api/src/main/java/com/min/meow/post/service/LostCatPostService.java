package com.min.meow.post.service;


import com.min.meow.global.PageResponse;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LostCatPostService {

    /**
     * 모든 실종 고양이 게시글 목록 조회 (Projection 적용)
     *
     * 성능 개선 내역:
     * - Before: Entity 전체 조회 + DTO 변환 → LazyInitializationException 발생 가능
     * - After: Projection으로 필요한 컬럼만 SELECT (id, title, writer, catName 등)
     *
     * @param pageable 페이징 정보
     * @return 경량화된 목록 응답 DTO
     */
    PageResponse<LostCatPostListResponse> getAllLostCatPosts(Pageable pageable);

    GetLostCatPostResponse getLostCatPost(Long lostCatPostId);

    CreateLostCatPostResponse createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest, String loginId);

    UpdateLostCatPostResponse updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest, String loginId);

    void deleteLostCatPost(Long lostCatPostId, String loginId, String password);

    /**
     * 메인페이지용: 최근 실종글 20개 조회 (DTO Projection 적용)
     *
     * 성능 최적화:
     * - QueryDSL Projection으로 필요한 컬럼만 SELECT
     * - Entity 변환 오버헤드 제거
     * - contents, imageUrls 등 불필요한 데이터 조회 제거
     *
     * Redis 캐싱이 적용되어 있으며, TTL은 5분입니다.
     *
     * @return 최근 실종글 20개 목록 (경량화된 DTO)
     */
    List<LostCatPostListResponse> getRecentLostCatPosts();

    /**
     * 조회수 증가 (별도 API)
     * 원자적 쿼리로 동시성 문제를 해결합니다.
     * @param lostCatPostId 게시글 ID
     */
    void incrementViewCount(Long lostCatPostId);
}
