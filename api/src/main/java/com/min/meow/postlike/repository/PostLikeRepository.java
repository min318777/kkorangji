package com.min.meow.postlike.repository;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.postlike.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("SELECT pl FROM PostLike pl WHERE pl.boastCatPost.id = :boastCatPostId AND pl.user.loginId = :loginId")
    Optional<PostLike> findByBoastCatPostIdAndLoginId(@Param("boastCatPostId") Long boastCatPostId, @Param("loginId") String loginId);

    //Optional<PostLike> findByBoastCatPostIdAndLoginId(Long boastCatPostId, String loginId);

    Integer countByBoastCatPost(BoastCatPost boastCatPost);

    // ==================== Redis 좋아요 서비스용 메서드 ====================

    /**
     * 특정 게시글에 좋아요한 모든 사용자 ID 조회 (캐시 워밍용)
     *
     * Redis SET에 로드할 사용자 ID 목록을 조회합니다.
     * 서버 시작 시 또는 캐시 미스 시 호출됩니다.
     *
     * @param boastCatPostId 게시글 ID
     * @return 좋아요한 사용자 ID 집합
     */
    @Query("SELECT pl.user.id FROM PostLike pl WHERE pl.boastCatPost.id = :boastCatPostId")
    Set<Long> findUserIdsByBoastCatPostId(@Param("boastCatPostId") Long boastCatPostId);

    /**
     * 특정 사용자의 특정 게시글 좋아요 여부 확인 (userId 기반)
     *
     * Redis 장애 시 fallback으로 사용됩니다.
     *
     * @param boastCatPostId 게시글 ID
     * @param userId 사용자 ID (PK)
     * @return 좋아요 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(pl) > 0 THEN true ELSE false END " +
            "FROM PostLike pl WHERE pl.boastCatPost.id = :boastCatPostId AND pl.user.id = :userId")
    boolean existsByBoastCatPostIdAndUserId(@Param("boastCatPostId") Long boastCatPostId, @Param("userId") Long userId);

    /**
     * 특정 게시글의 좋아요 수 조회 (postId 기반)
     *
     * Redis 장애 시 fallback으로 사용됩니다.
     *
     * @param boastCatPostId 게시글 ID
     * @return 좋아요 수
     */
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.boastCatPost.id = :boastCatPostId")
    Long countByBoastCatPostId(@Param("boastCatPostId") Long boastCatPostId);
}
