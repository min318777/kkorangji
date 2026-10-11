package com.min.meow.user.repository;

import com.min.meow.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    // 특정 User에 연결된 UserRole 전체 삭제 — findAll() 없이 DB 레벨에서 단건 DELETE
    @Modifying
    @Transactional
    @Query("DELETE FROM UserRole ur " +
           "WHERE ur.user.id = :userId")
    void deleteByUserId(Long userId);
}
