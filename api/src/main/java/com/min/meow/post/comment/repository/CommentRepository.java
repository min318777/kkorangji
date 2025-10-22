package com.min.meow.post.comment.repository;

import com.min.meow.post.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long > {

    // 고양이 자랑 게시글의 댓글 조회
    @Query("SELECT c FROM Comment c WHERE c.boastCatPost.id = :postId ORDER BY c.createdAt DESC")
    List<Comment> findByBoastCatPostIdWithUser(@Param("postId") Long postId);

    // 실종 고양이 게시글의 댓글 조회
    @Query("SELECT c FROM Comment c WHERE c.lostCatPost.id = :postId ORDER BY c.createdAt DESC")
    List<Comment> findByLostCatPostIdWithUser(@Param("postId") Long postId);

}
