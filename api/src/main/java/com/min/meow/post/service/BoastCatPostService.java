package com.min.meow.post.service;

import com.min.meow.global.PageResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.RecentBoastCatPostResponse;
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

    List<RecentBoastCatPostResponse> getRecentBoastCatPosts();

    /**
     * 조회수 증가 (별도 API)
     * 원자적 쿼리로 동시성 문제를 해결합니다.
     * @param boastCatPostId 게시글 ID
     */
    void incrementViewCount(Long boastCatPostId);
}
