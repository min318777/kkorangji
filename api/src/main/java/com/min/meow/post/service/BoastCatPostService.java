package com.min.meow.post.service;

import com.min.meow.global.PageResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BoastCatPostService {

    /**
     * 게시글 목록 조회 (Projection 적용)
     * - 목록에서 필요한 필드만 조회 (title, writer, likeCount, commentCount, view, createdAt)
     * - contents, imageUrls, comments 제외하여 성능 최적화
     */
    PageResponse<BoastCatPostListResponse> getAllBoastCatPosts(Pageable pageable);

    GetBoastCatPostResponse getBoastCatPost(Long boastCatPostId);

    CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, String loginId);

    UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest,Long boastCatPostId, String loginId);

    void deleteBoastCatPost(Long boastCatPostId, String loginId, String password);

    /**
     * 메인페이지용: 최근 자랑글 20개 조회 (DTO Projection 적용)
     *
     * 성능 최적화:
     * - QueryDSL Projection으로 필요한 컬럼만 SELECT
     * - Entity 변환 오버헤드 제거
     * - contents, imageUrls 등 불필요한 데이터 조회 제거
     * - BoastCatPostListResponse를 재사용하여 코드 중복 제거
     *
     * Redis 캐싱이 적용되어 있으며, TTL은 5분입니다.
     *
     * @return 최근 자랑글 20개 목록 (경량화된 DTO)
     */
    List<BoastCatPostListResponse> getRecentBoastCatPosts();

    /**
     * 조회수 증가 (별도 API)
     * 원자적 쿼리로 동시성 문제를 해결합니다.
     * @param boastCatPostId 게시글 ID
     */
    void incrementViewCount(Long boastCatPostId);
}
