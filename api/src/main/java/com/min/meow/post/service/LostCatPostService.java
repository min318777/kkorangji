package com.min.meow.post.service;


import com.min.meow.global.PageResponse;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LostCatPostService {

    PageResponse<GetLostCatPostResponse> getAllLostCatPosts(Pageable pageable);

    GetLostCatPostResponse getLostCatPost(Long lostCatPostId);

    CreateLostCatPostResponse createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest, String loginId);

    UpdateLostCatPostResponse updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest, String loginId);

    void deleteLostCatPost(Long lostCatPostId, String loginId, String password);

    /**
     * 메인페이지용: 최근 실종글 20개 조회
     * Redis 캐싱이 적용되어 있으며, TTL은 1분입니다.
     * 캐싱 테스트 시 @Cacheable 어노테이션을 주석 처리하여 비교할 수 있습니다.
     * @return 최근 실종글 20개 목록
     */
    List<GetLostCatPostResponse> getRecentLostCatPosts();
}
