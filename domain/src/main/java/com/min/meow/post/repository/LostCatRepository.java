package com.min.meow.post.repository;

import com.min.meow.post.entity.LostCatPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LostCatRepository extends JpaRepository<LostCatPost, Long> {

    @Query("SELECT lo FROM LostCatPost lo LEFT JOIN FETCH lo.comments")
    List<LostCatPost> findAllFetch(Pageable pageable);

    @Query("SELECT DISTINCT p FROM LostCatPost p LEFT JOIN FETCH p.imageUrls ORDER BY p.createdAt DESC")
    List<LostCatPost> findAllWithImageUrls();

    // 마이페이지: 사용자가 작성한 실종 고양이글 목록 조회 (페이징)
    // 참고: comments는 지연 로딩되지만, DTO 변환 시 size()로 개수만 조회하므로 문제없음
    @Query("SELECT l FROM LostCatPost l " +
           "WHERE l.user.id = :userId " +
           "ORDER BY l.createdAt DESC")
    Page<LostCatPost> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
