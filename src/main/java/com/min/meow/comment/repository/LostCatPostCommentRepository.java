package com.min.meow.comment.repository;

import com.min.meow.comment.entity.LostCatPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LostCatPostCommentRepository extends JpaRepository<LostCatPostComment, Long > {
}
