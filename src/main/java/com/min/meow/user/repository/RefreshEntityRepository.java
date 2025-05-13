package com.min.meow.user.repository;


import com.min.meow.global.Token;
import com.min.meow.user.entity.RefreshEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Transactional
@Repository
public interface RefreshEntityRepository extends JpaRepository<RefreshEntity, Long> {

    boolean existsByRefreshToken(String token);

    void deleteByRefreshToken(String token);
}
