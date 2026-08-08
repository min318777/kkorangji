package com.min.meow.user.repository;

import com.min.meow.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);

    /**
     * 역할 필터를 지원하는 유저 목록 페이징 조회.
     * FETCH JOIN + Pageable 충돌 방지를 위해 서브쿼리 방식 사용.
     * roleName이 null이면 전체 유저 반환.
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:roleName IS NULL OR u.id IN " +
           "  (SELECT ur.user.id FROM UserRole ur WHERE ur.role.name = :roleName))")
    Page<User> findAllByOptionalRole(@Param("roleName") String roleName, Pageable pageable);

    Optional<User> findByLoginId(String loginId);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(b) FROM BoastCatPost b " +
           "WHERE b.user.id = :userId")
    long countBoastCatPostsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM LostCatPost l " +
           "WHERE l.user.id = :userId")
    long countLostCatPostsByUserId(@Param("userId") Long userId);

}
