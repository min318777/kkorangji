package com.min.meow.post.repository;

import com.min.meow.post.entity.BoastCatPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoastCatPostRepository extends JpaRepository<BoastCatPost, Long>, BoastCatPostRepositoryCustom {

    Page<BoastCatPost> findAll(Pageable pageable);

    // 모든 게시글 + 이미지Urls 한 번에 가져오기
    @Query("SELECT DISTINCT p FROM BoastCatPost p LEFT JOIN FETCH p.imageUrls ORDER BY p.createdAt DESC")
    List<BoastCatPost> findAllWithImageUrls();

    // 단일 게시글 조회 시
    @Query("SELECT p FROM BoastCatPost p LEFT JOIN FETCH p.imageUrls WHERE p.id = :id")
    Optional<BoastCatPost> findByIdWithImages(@Param("id") Long id);

    // 마이페이지: 사용자가 작성한 고양이 자랑글 목록 조회 (페이징)
    // 참고: comments와 postLikeList는 지연 로딩되지만, DTO 변환 시 size()로 개수만 조회하므로 문제없음
    @Query("SELECT b FROM BoastCatPost b " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.createdAt DESC")
    Page<BoastCatPost> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
