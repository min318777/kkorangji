package com.min.meow.comment.repository;

import com.min.meow.comment.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<PostComment, Long > {
}
