package com.min.meow.boastcatpost.repository;


import com.min.meow.boastcatpost.entity.BoastCatPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoastCatPostRepository extends JpaRepository<BoastCatPost, Long> {


}
