package com.min.meow.user.repository;


import com.min.meow.user.entity.RefreshToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Transactional
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    boolean existsByRefreshToken(String token);

    void deleteByRefreshToken(String token);


    RefreshToken findByRefreshToken(String token);

    void deleteByLoginId(String loginId);
}
