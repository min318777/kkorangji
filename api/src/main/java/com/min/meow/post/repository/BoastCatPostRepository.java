package com.min.meow.post.repository;

import com.min.meow.post.entity.BoastCatPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoastCatPostRepository extends JpaRepository<BoastCatPost, Long>, BoastCatPostRepositoryCustom {

    /**
     * 단일 게시글 상세 조회 (N+1 문제 해결)
     * User는 @ManyToOne (N:1) 관계이므로 Fetch Join 적용
     * - 결과 행이 뻥튀기되지 않아 안전함
     * - 한 번의 쿼리로 게시글 + 작성자 정보 조회
     * imageUrls, comments는 @OneToMany (1:N) 관계이므로 @BatchSize로 해결
     * - Fetch Join 시 카테시안 곱 발생 (데이터 중복)
     * - 두 개 이상 컬렉션 Fetch Join 시 MultipleBagFetchException 발생
     * - @BatchSize(100)로 IN절 배치 처리하여 추가 쿼리 최소화
     *
     * 쿼리 최적화 결과: 4개 → 3개
     * - Before: Post 1 + User 1 + ImageUrls 1 + Comments 1 = 4개
     * - After: Post+User 1 + ImageUrls 1 + Comments 1 = 3개
     */
    @Query("SELECT p FROM BoastCatPost p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id = :id")
    Optional<BoastCatPost> findByIdWithUser(@Param("id") Long id);

    // 마이페이지: 사용자가 작성한 고양이 자랑글 목록 조회 (페이징)
    // 참고: comments와 postLikeList는 지연 로딩되지만, DTO 변환 시 size()로 개수만 조회하므로 문제없음
    @Query("SELECT b FROM BoastCatPost b " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.createdAt DESC")
    Page<BoastCatPost> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);


    /**
     * 조회수 원자적 증가 (동시성 문제 해결)
     * DB 레벨에서 view = view + 1을 수행하여 Race Condition을 방지합니다.
     * 여러 스레드가 동시에 호출해도 정확한 조회수가 보장됩니다.
     * @param id 게시글 ID
     * @return 업데이트된 행의 수 (정상: 1, 게시글 없음: 0)
     */
    @Modifying
    @Query("UPDATE BoastCatPost b SET b.view = b.view + 1 WHERE b.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
