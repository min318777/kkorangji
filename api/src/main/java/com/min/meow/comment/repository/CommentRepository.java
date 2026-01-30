package com.min.meow.comment.repository;

import com.min.meow.comment.entity.Comment;
import com.min.meow.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 마이페이지: 사용자가 작성한 댓글 목록 조회 (페이징, 게시글 정보 함께 로딩)
    @Query("SELECT c FROM Comment c " +
           "LEFT JOIN FETCH c.boastCatPost " +
           "LEFT JOIN FETCH c.lostCatPost " +
           "WHERE c.user = :user " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    // 사용자가 작성한 댓글의 총 개수 조회
    long countByUser(User user);


}
