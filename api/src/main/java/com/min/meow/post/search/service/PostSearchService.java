package com.min.meow.post.search.service;


import com.min.meow.post.search.domain.PostDto;
import com.min.meow.post.search.domain.RestPage;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostSearchService {

    RestPage<PostDto> search(PostSearchRequest postSearchRequest, Pageable pageable);
}
