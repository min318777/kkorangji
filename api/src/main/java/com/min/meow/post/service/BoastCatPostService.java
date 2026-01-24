package com.min.meow.post.service;

import com.min.meow.global.PageResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BoastCatPostService {

    PageResponse<GetBoastCatPostResponse> getAllBoastCatPosts(Pageable pageable);

    GetBoastCatPostResponse getBoastCatPost(Long boastCatPostId);

    CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, String loginId);

    UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest,Long boastCatPostId, String loginId);

    void deleteBoastCatPost(Long boastCatPostId, String loginId, String password);

    /**
     * 메인페이지용: 최근 자랑글 20개 조회
     * Redis 캐싱이 적용되어 있으며, TTL은 1분입니다.
     * 캐싱 테스트 시 @Cacheable 어노테이션을 주석 처리하여 비교할 수 있습니다.
     * @return 최근 자랑글 20개 목록
     */
    List<GetBoastCatPostResponse> getRecentBoastCatPosts();
}
