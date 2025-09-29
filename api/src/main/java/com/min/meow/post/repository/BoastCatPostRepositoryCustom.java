package com.min.meow.post.repository;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;


public interface BoastCatPostRepositoryCustom {

    Page<BoastCatPost> search(PostSearchRequest postSearchRequest, Pageable pageable);


}
