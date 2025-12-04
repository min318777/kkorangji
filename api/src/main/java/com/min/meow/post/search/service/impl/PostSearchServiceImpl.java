package com.min.meow.post.search.service.impl;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepositoryImpl;
import com.min.meow.post.search.domain.PostDto;
import com.min.meow.post.search.domain.Page;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import com.min.meow.post.search.service.PostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostSearchServiceImpl implements PostSearchService {

    private final BoastCatPostRepositoryImpl boastCatPostRepositoryImpl;

    @Override
    public Page<PostDto> search(PostSearchRequest postSearchRequest, Pageable pageable) {
        // Repository 메서드 시그니처 변경에 맞춰 파라미터 전달
        org.springframework.data.domain.Page<BoastCatPost> boastCatPosts = boastCatPostRepositoryImpl.search(
                postSearchRequest.getTitle(),
                postSearchRequest.getContents(),
                pageable
        );

        // toDto 메서드 제거됨 - 직접 변환
        org.springframework.data.domain.Page<PostDto> postDtos = boastCatPosts.map(post ->
                PostDto.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .contents(post.getContents())
                        .userId(post.getUser().getId())
                        .view(post.getView())
                        .createdAt(post.getCreatedAt())
                        .updatedAt(post.getUpdatedAt())
                        .build()
        );
        return new Page<>(postDtos);
    }
}
