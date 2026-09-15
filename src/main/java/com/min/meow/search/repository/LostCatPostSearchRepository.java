package com.min.meow.search.repository;

import com.min.meow.post.entity.LostCatPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LostCatPostSearchRepository extends JpaRepository<LostCatPost, Long>, LostCatPostSearchRepositoryCustom {

    // LIKE 검색: 제목 또는 내용에 keyword 포함 ('%keyword%' 방식, JPA 메서드 이름 쿼리)
    Page<LostCatPost> findByTitleContainingOrContentsContaining(String title, String contents, Pageable pageable);
}
