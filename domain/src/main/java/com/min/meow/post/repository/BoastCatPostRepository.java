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

import java.util.List;
import java.util.Optional;

@Repository
public interface BoastCatPostRepository extends JpaRepository<BoastCatPost, Long>, BoastCatPostRepositoryCustom {

    /**
     * 페이징 목록 조회 (N+1 문제 해결)
     * User를 Fetch Join으로 함께 조회하여 N+1 문제를 방지합니다.
     * countQuery를 별도로 지정하여 페이징 카운트 쿼리 최적화합니다.
     */
    @Query(value = "SELECT b FROM BoastCatPost b LEFT JOIN FETCH b.user ORDER BY b.createdAt DESC",
           countQuery = "SELECT COUNT(b) FROM BoastCatPost b")
    Page<BoastCatPost> findAllWithUser(Pageable pageable);

    // 단일 게시글 조회 시
    @Query("SELECT p FROM BoastCatPost p LEFT JOIN FETCH p.imageUrls WHERE p.id = :id")
    Optional<BoastCatPost> findByIdWithImages(@Param("id") Long id);

    // 마이페이지: 사용자가 작성한 고양이 자랑글 목록 조회 (페이징)
    // 참고: comments와 postLikeList는 지연 로딩되지만, DTO 변환 시 size()로 개수만 조회하므로 문제없음
    @Query("SELECT b FROM BoastCatPost b " +
           "WHERE b.user.id = :userId " +
           "ORDER BY b.createdAt DESC")
    Page<BoastCatPost> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 메인페이지용: 최근 자랑글 20개 조회
     * N+1 문제 방지를 위해 User를 Fetch Join으로 함께 조회합니다.
     * imageUrls는 ElementCollection이므로 별도 쿼리로 조회됩니다.
     * 정렬: 최신순 (createdAt DESC)
     */
    @Query("SELECT DISTINCT b FROM BoastCatPost b " +
           "LEFT JOIN FETCH b.user " +
           "LEFT JOIN FETCH b.imageUrls " +
           "ORDER BY b.createdAt DESC " +
           "LIMIT 20")
    List<BoastCatPost> findTop20RecentPosts();

    /**
     * 조회수 원자적 증가 (동시성 문제 해결)
     *
     * DB 레벨에서 view = view + 1을 수행하여 Race Condition을 방지합니다.
     * 여러 스레드가 동시에 호출해도 정확한 조회수가 보장됩니다.
     *
     * @param id 게시글 ID
     * @return 업데이트된 행의 수 (정상: 1, 게시글 없음: 0)
     */
    @Modifying
    @Query("UPDATE BoastCatPost b SET b.view = b.view + 1 WHERE b.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
