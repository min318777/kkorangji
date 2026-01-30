package com.min.meow.user.repository;

import com.min.meow.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginId(String loginId);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * 바로 n+1해결함 n+1전의 쿼리를 쓰고 성능비교해보자
     * @param userId
     * @return
     */
    @Query("SELECT COUNT(b) FROM BoastCatPost b WHERE b.user.id = :userId")
    long countBoastCatPostsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM LostCatPost l WHERE l.user.id = :userId")
    long countLostCatPostsByUserId(@Param("userId") Long userId);

    // Comment 엔티티의 user 관계를 통해 loginId로 댓글 수 조회
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.loginId = :loginId")
    long countCommentsByWriter(@Param("loginId") String loginId);
}
