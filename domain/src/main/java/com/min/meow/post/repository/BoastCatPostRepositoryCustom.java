package com.min.meow.post.repository;

import com.min.meow.post.entity.BoastCatPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoastCatPostRepositoryCustom {

    // 검색 메서드: title과 contents를 직접 파라미터로 받음
    Page<BoastCatPost> search(String title, String contents, Pageable pageable);

}