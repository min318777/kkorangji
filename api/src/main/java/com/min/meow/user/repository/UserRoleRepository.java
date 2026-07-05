package com.min.meow.user.repository;

import com.min.meow.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    // 특정 User에 연결된 UserRole 목록 조회 — 테스트 tearDown에서 활용
    List<UserRole> findByUserId(Long userId);

    // 특정 User에 연결된 UserRole 전체 삭제 — findAll() 없이 DB 레벨에서 단건 DELETE
    // @Modifying + @Transactional: DELETE 쿼리는 트랜잭션 컨텍스트가 필요함
    @Modifying
    @Transactional
    @Query("DELETE FROM UserRole ur " +
           "WHERE ur.user.id = :userId")
    void deleteByUserId(Long userId);
}
