package com.min.meow.comment.repository;

import com.min.meow.comment.entity.Comment;
import com.min.meow.common.PostType;
import com.min.meow.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 게시글 원댓글 조회 (postId + postType 통합, 작성자 fetch join)
    @Query(value = "SELECT c FROM Comment c " +
                   "JOIN FETCH c.user " +
                   "WHERE c.postId = :postId AND c.postType = :postType AND c.parentComment IS NULL " +
                   "ORDER BY c.createdAt DESC, c.id DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c " +
                        "WHERE c.postId = :postId AND c.postType = :postType AND c.parentComment IS NULL")
    Page<Comment> findRootByPostIdAndPostType(@Param("postId") Long postId,
                                              @Param("postType") PostType postType,
                                              Pageable pageable);

    // 여러 원댓글의 대댓글 일괄 조회 (쿼리 1번으로 N+1 방지)
    @Query("SELECT c FROM Comment c " +
           "JOIN FETCH c.user " +
           "WHERE c.parentComment.id IN :parentIds " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    // 활성 대댓글 수 확인 (삭제 여부 판단용)
    @Query("SELECT COUNT(c) FROM Comment c " +
           "WHERE c.parentComment.id = :parentId AND c.isDeleted = false")
    long countActiveRepliesByParentId(@Param("parentId") Long parentId);

    // 마이페이지: 사용자가 작성한 댓글 목록 조회
    @Query(value = "SELECT c FROM Comment c " +
                   "WHERE c.user = :user " +
                   "ORDER BY c.createdAt DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c " +
                        "WHERE c.user = :user")
    Page<Comment> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    // 사용자가 작성한 댓글 총 개수
    long countByUserId(Long userId);

    // 게시글 삭제 시 연관 댓글 일괄 삭제 (cascade 대체)
    void deleteAllByPostIdAndPostType(Long postId, PostType postType);
}
