package com.min.meow.post.search.service.impl;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepositoryImpl;
import com.min.meow.post.search.domain.PostDto;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import com.min.meow.post.search.service.PostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostSearchServiceImpl implements PostSearchService {

    private final BoastCatPostRepositoryImpl boastCatPostRepositoryImpl;

    @Override
    public Page<PostDto> search(PostSearchRequest postSearchRequest, Pageable pageable) {

        Page<BoastCatPost> boastCatPosts = boastCatPostRepositoryImpl.search(
                postSearchRequest.getTitle(),
                postSearchRequest.getContents(),
                pageable
        );

        return boastCatPosts.map(post ->
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
    }
}
