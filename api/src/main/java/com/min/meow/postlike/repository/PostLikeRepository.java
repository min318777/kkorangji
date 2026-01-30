package com.min.meow.postlike.repository;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.postlike.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("SELECT pl FROM PostLike pl WHERE pl.boastCatPost.id = :boastCatPostId AND pl.user.loginId = :loginId")
    Optional<PostLike> findByBoastCatPostIdAndLoginId(@Param("boastCatPostId") Long boastCatPostId, @Param("loginId") String loginId);

    //Optional<PostLike> findByBoastCatPostIdAndLoginId(Long boastCatPostId, String loginId);

    Integer countByBoastCatPost(BoastCatPost boastCatPost);
}
