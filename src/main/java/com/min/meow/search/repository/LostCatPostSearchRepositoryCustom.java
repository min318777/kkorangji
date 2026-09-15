package com.min.meow.search.repository;

import com.min.meow.post.dto.response.LostCatPostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LostCatPostSearchRepositoryCustom {

    // FTS 검색: MATCH AGAINST (ngram)
    Page<LostCatPostListResponse> searchByKeyword(String keyword, Pageable pageable);
}
