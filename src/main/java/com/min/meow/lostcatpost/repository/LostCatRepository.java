package com.min.meow.lostcatpost.repository;

import com.min.meow.lostcatpost.entity.LostCatPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostCatRepository extends JpaRepository<LostCatPost, Long> {
}
