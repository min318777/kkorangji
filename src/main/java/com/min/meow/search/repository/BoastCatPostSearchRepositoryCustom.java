package com.min.meow.search.repository;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoastCatPostSearchRepositoryCustom {

    // Full-Text Search (ngram): MATCH(title, contents) AGAINST(keyword IN BOOLEAN MODE)
    Page<BoastCatPostListResponse> searchByKeyword(String keyword, Pageable pageable);

    // Full-Text Search (ngram): MATCH(title, contents) AGAINST(keyword IN NATURAL LANGUAGE MODE)
    Page<BoastCatPostListResponse> searchByNaturalLanguage(String keyword, Pageable pageable);
}
