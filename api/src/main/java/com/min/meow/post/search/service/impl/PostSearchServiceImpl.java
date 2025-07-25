package com.min.meow.post.search.service.impl;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepositoryImpl;
import com.min.meow.post.search.domain.PostDto;
import com.min.meow.post.search.domain.RestPage;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import com.min.meow.post.search.service.PostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostSearchServiceImpl implements PostSearchService {

    private final BoastCatPostRepositoryImpl boastCatPostRepositoryImpl;

    @Cacheable(value = "search", key = "'getPosts' + #postSearchRequest.getTitle()")
    @Override
    public RestPage<PostDto> search(PostSearchRequest postSearchRequest, Pageable pageable) {
        Page<BoastCatPost> boastCatPosts = boastCatPostRepositoryImpl.search(postSearchRequest, pageable);
        Page<PostDto> postDtos = boastCatPosts.map(BoastCatPost::toDto);
        return new RestPage<>(postDtos);
    }
}
