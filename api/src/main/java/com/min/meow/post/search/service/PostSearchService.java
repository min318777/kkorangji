package com.min.meow.post.search.service;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.repository.BoastCatPostRepositoryImpl;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchService {

    private final BoastCatPostRepositoryImpl boastCatPostRepositoryImpl;

    /**
     * 게시글 검색 - DB에서 바로 DTO로 조회 (Projection)
     * Entity → PostDto 수동 변환 제거, BoastCatPostListResponse 직접 반환
     */
    public Page<BoastCatPostListResponse> search(PostSearchRequest postSearchRequest, Pageable pageable) {
        return boastCatPostRepositoryImpl.search(
                postSearchRequest.getTitle(),
                postSearchRequest.getContents(),
                postSearchRequest.getUserId(),
                pageable
        );
    }
}
