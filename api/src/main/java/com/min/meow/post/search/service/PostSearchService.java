package com.min.meow.post.search.service;


import com.min.meow.post.search.domain.PostDto;
import com.min.meow.post.search.domain.Page;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import org.springframework.data.domain.Pageable;

public interface PostSearchService {

    Page<PostDto> search(PostSearchRequest postSearchRequest, Pageable pageable);
}
