package com.min.meow.post.repository;

import com.min.meow.post.entity.LostCatPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LostCatRepository extends JpaRepository<LostCatPost, Long>, LostCatRepositoryCustom {

    // 단일 게시글 상세 조회 — User(N:1)는 Fetch Join, imageUrls(1:N)는 카테시안 곱/MultipleBagFetchException 위험이 있어 제외
    @Query("SELECT l FROM LostCatPost l " +
            "LEFT JOIN FETCH l.user " +
            "WHERE l.id = :id")
    Optional<LostCatPost> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT l FROM LostCatPost l " +
           "WHERE l.user.id = :userId " +
           "ORDER BY l.createdAt DESC")
    Page<LostCatPost> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // ========== 조회수 ==========

    // 조회수 원자적 증가
    @Modifying
    @Query("UPDATE LostCatPost l " +
           "SET l.view = l.view + 1 " +
           "WHERE l.id = :id")
    int incrementViewCount(@Param("id") Long id);

    // ========== 댓글 수 ==========

    // 댓글 수 증가
    @Modifying
    @Query("UPDATE LostCatPost l " +
           "SET l.commentCount = l.commentCount + 1 " +
           "WHERE l.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    // 댓글 수 감소
    @Modifying
    @Query("UPDATE LostCatPost l " +
           "SET l.commentCount = CASE WHEN l.commentCount > 0 THEN l.commentCount - 1 ELSE 0 END " +
           "WHERE l.id = :id")
    void decrementCommentCount(@Param("id") Long id);
}
