package com.min.meow.lostcatpost.repository;

import com.min.meow.lostcatpost.entity.LostCatPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LostCatRepository extends JpaRepository<LostCatPost, Long> {


    @Query("SELECT lo FROM LostCatPost lo LEFT JOIN FETCH lo.postComments")
    List<LostCatPost> findAllFetch(Pageable pageable);

}
