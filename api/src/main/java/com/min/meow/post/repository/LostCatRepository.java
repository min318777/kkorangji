package com.min.meow.post.repository;

import com.min.meow.post.entity.LostCatPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LostCatRepository extends JpaRepository<LostCatPost, Long> {


    @Query("SELECT lo FROM LostCatPost lo LEFT JOIN FETCH lo.comments")
    List<LostCatPost> findAllFetch(Pageable pageable);

}
