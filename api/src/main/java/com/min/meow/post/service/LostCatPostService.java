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

    void deleteLostCatPost(Long lostCatPostId, String loginId);

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
     * 조회수 증가 - 원자적 쿼리 방식 (v2 - 개선된 버전)
     *
     * DB 레벨에서 view = view + 1을 수행하여 동시성 문제를 해결합니다.
     * K6 동시성 테스트에서 더티 체킹 방식의 Lost Update 문제를 발견한 후 개선한 버전입니다.
     *
     * @param lostCatPostId 게시글 ID
     */
    void incrementViewCount(Long lostCatPostId);

    /**
     * 조회수 증가 - 더티 체킹 방식 (v1 - 동시성 이슈 있음)
     *
     * ⚠️ 동시성 문제 (Lost Update):
     * JPA 더티 체킹을 사용한 조회수 증가 방식입니다.
     * read-modify-write 패턴으로 동시 요청 시 일부 업데이트가 손실됩니다.
     *
     * 문제 시나리오:
     * 1. 트랜잭션 A: SELECT로 view=100 조회
     * 2. 트랜잭션 B: SELECT로 view=100 조회
     * 3. 트랜잭션 A: view++ → UPDATE view=101
     * 4. 트랜잭션 B: view++ → UPDATE view=101 (A의 업데이트 덮어씀)
     * 5. 결과: 2회 요청 → 1만 증가 (Lost Update)
     *
     * K6 동시성 테스트로 발견 후 incrementViewCount()로 개선하였습니다.
     *
     * @param lostCatPostId 게시글 ID
     * @deprecated 동시성 이슈로 인해 incrementViewCount() 사용 권장
     */
    void incrementViewCountWithDirtyChecking(Long lostCatPostId);
}
