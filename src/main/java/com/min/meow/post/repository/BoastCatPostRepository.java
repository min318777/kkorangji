package com.min.meow.post.repository;

import com.min.meow.post.entity.BoastCatPost;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoastCatPostRepository extends JpaRepository<BoastCatPost, Long>, BoastCatPostRepositoryCustom {

    // 단일 게시글 상세 조회 — imageUrls(1:N)는 Fetch Join 시 카테시안 곱/MultipleBagFetchException 위험이 있어 제외
    @Query("SELECT p FROM BoastCatPost p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id = :id")
    Optional<BoastCatPost> findByIdWithUser(@Param("id") Long id);

    // v3(비교군): 비관적 락(SELECT FOR UPDATE)으로 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM BoastCatPost p LEFT JOIN FETCH p.user WHERE p.id = :id")
    Optional<BoastCatPost> findByIdForUpdate(@Param("id") Long id);

    // 마이페이지: 사용자가 작성한 고양이 자랑글 목록 조회 (페이징)
    @Query("SELECT b FROM BoastCatPost b " +
            "WHERE b.user.id = :userId " +
            "ORDER BY b.createdAt DESC")
    Page<BoastCatPost> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // ========== 조회수 ==========

    // 조회수 원자적 증가
    @Modifying
    @Query("UPDATE BoastCatPost b " +
            "SET b.view = b.view + 1 " +
            "WHERE b.id = :id")
    int incrementViewCount(@Param("id") Long id);

    // ========== 댓글 수 ==========

    // 댓글 수 증가
    @Modifying
    @Query("UPDATE BoastCatPost b " +
            "SET b.commentCount = b.commentCount + 1 " +
            "WHERE b.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    // 댓글 수 감소
    @Modifying
    @Query("UPDATE BoastCatPost b " +
            "SET b.commentCount = CASE WHEN b.commentCount > 0 THEN b.commentCount - 1 ELSE 0 END " +
            "WHERE b.id = :id")
    void decrementCommentCount(@Param("id") Long id);

    // ========== 좋아요 수 ==========

    // 좋아요 수 증가/감소
    @Modifying
    @Query("UPDATE BoastCatPost b " +
            "SET b.likeCount = CASE WHEN b.likeCount + :delta < 0 THEN 0 ELSE b.likeCount + :delta END " +
            "WHERE b.id = :id")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    // updateLikeCount 반영 후 최신 좋아요 수 조회 (UPDATE는 영향받은 row 수만 반환하므로 재조회 필요)
    @Query("SELECT b.likeCount FROM BoastCatPost b WHERE b.id = :id")
    int findLikeCountById(@Param("id") Long id);
}