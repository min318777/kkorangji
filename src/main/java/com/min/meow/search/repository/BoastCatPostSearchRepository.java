package com.min.meow.search.repository;

import com.min.meow.post.entity.BoastCatPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoastCatPostSearchRepository extends JpaRepository<BoastCatPost, Long>, BoastCatPostSearchRepositoryCustom {

    // LIKE 검색: 제목 또는 내용에 keyword 포함
    Page<BoastCatPost> findByTitleContainingOrContentsContaining(String title, String contents, Pageable pageable);
}
