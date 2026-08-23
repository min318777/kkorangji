package com.min.meow.postlike.repository;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.postlike.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /**
     * 특정 게시글에 좋아요한 모든 사용자 ID 조회 (캐시 워밍용)
     * Redis SET에 로드할 사용자 ID 목록을 조회합니다.
     * 서버 시작 시 또는 캐시 미스 시 호출됩니다.
     * @param boastCatPostId 게시글 ID
     * @return 좋아요한 사용자 ID 집합
     */
    @Query("SELECT pl.user.id FROM PostLike pl " +
           "WHERE pl.boastCatPost.id = :boastCatPostId")
    Set<Long> findUserIdsByBoastCatPostId(@Param("boastCatPostId") Long boastCatPostId);

    /**
     * 좋아요 삭제 (userId + postId 기반)
     * 좋아요 취소 시 사용됩니다.
     */
    @Modifying
    @Query("DELETE FROM PostLike pl " +
           "WHERE pl.boastCatPost.id = :postId AND pl.user.id = :userId")
    void deleteByBoastCatPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * 특정 사용자의 특정 게시글 좋아요 여부 확인 (userId 기반)
     * Redis 장애 시 fallback으로 사용됩니다.
     * @param boastCatPostId 게시글 ID
     * @param userId 사용자 ID (PK)
     * @return 좋아요 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(pl) > 0 THEN true ELSE false END " +
           "FROM PostLike pl " +
           "WHERE pl.boastCatPost.id = :boastCatPostId AND pl.user.id = :userId")
    boolean existsByBoastCatPostIdAndUserId(@Param("boastCatPostId") Long boastCatPostId, @Param("userId") Long userId);

    /**
     * 특정 게시글의 좋아요 수 조회 (postId 기반)
     * Redis 장애 시 fallback으로 사용됩니다.
     * @param boastCatPostId 게시글 ID
     * @return 좋아요 수
     */
    @Query("SELECT COUNT(pl) FROM PostLike pl " +
           "WHERE pl.boastCatPost.id = :boastCatPostId")
    Long countByBoastCatPostId(@Param("boastCatPostId") Long boastCatPostId);

    // 특정 사용자가 좋아요한 BoastCatPost 목록 (마이페이지용, 최근 좋아요 순)
    @Query(value = "SELECT pl.boastCatPost FROM PostLike pl " +
                   "WHERE pl.user.id = :userId " +
                   "ORDER BY pl.id DESC",
           countQuery = "SELECT COUNT(pl) FROM PostLike pl " +
                        "WHERE pl.user.id = :userId")
    Page<BoastCatPost> findLikedBoastCatPostsByUserId(@Param("userId") Long userId, Pageable pageable);
}
