package com.min.meow.post.boastcatpost.repository;


import com.min.meow.post.boastcatpost.entity.BoastCatPost;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface BoastCatPostRepositoryCustom {

    Page<BoastCatPost> search(PostSearchRequest postSearchRequest, Pageable pageable);
}
