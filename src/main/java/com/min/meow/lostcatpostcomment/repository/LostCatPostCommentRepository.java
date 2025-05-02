package com.min.meow.lostcatpostcomment.repository;

import com.min.meow.lostcatpostcomment.entity.LostCatPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LostCatPostCommentRepository extends JpaRepository<LostCatPostComment, Long > {
}
