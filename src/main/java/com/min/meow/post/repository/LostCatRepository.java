package com.min.meow.post.repository;

import com.min.meow.post.entity.LostCatPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LostCatRepository extends JpaRepository<LostCatPost, Long>, LostCatRepositoryCustom {

    // ========== 조회 ==========

    /**
     * 단일 게시글 상세 조회
     * User는 @ManyToOne (N:1) 관계이므로 Fetch Join 적용
     * - 결과 행이 뻥튀기되지 않아 안전함
     * - 한 번의 쿼리로 게시글 + 작성자 정보 조회
     * imageUrls는 @OneToMany (1:N) 관계라 Fetch Join하지 않음
     * - User와 함께 Fetch Join 시 카테시안 곱 발생 (데이터 중복)
     * - 두 개 이상 컬렉션을 동시에 Fetch Join하면 MultipleBagFetchException 발생
     */
    @Query("SELECT l FROM LostCatPost l " +
            "LEFT JOIN FETCH l.user " +
            "WHERE l.id = :id")
    Optional<LostCatPost> findByIdWithUser(@Param("id") Long id);

    // 마이페이지: 사용자가 작성한 실종 고양이글 목록 조회 (페이징)
    // 참고: comments는 지연 로딩되지만, DTO 변환 시 size()로 개수만 조회하므로 문제없음
    @Query("SELECT l FROM LostCatPost l " +
           "WHERE l.user.id = :userId " +
           "ORDER BY l.createdAt DESC")
    Page<LostCatPost> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // ========== 조회수 ==========

    /**
     * 조회수 원자적 증가 (v2 — 동시성 문제 해결)
     * DB 레벨에서 view = view + 1을 수행하여 Race Condition을 방지합니다.
     * 여러 스레드가 동시에 호출해도 정확한 조회수가 보장됩니다.
     * @param id 게시글 ID
     * @return 업데이트된 행의 수 (정상: 1, 게시글 없음: 0)
     */
    @Modifying
    @Query("UPDATE LostCatPost l " +
           "SET l.view = l.view + 1 " +
           "WHERE l.id = :id")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 조회수 델타값 일괄 증가 (Redis → DB 동기화용)
     * Redis에 누적된 조회수를 DB에 한 번에 반영합니다.
     * 스케줄러에 의해 주기적으로 호출됩니다.
     * 실행되는 쿼리:
     * UPDATE lost_cat_post SET view = view + :delta WHERE id = :id
     * @param id 게시글 ID
     * @param delta 증가시킬 조회수
     * @return 업데이트된 행의 수
     */
    @Modifying
    @Query("UPDATE LostCatPost l " +
           "SET l.view = l.view + :delta " +
           "WHERE l.id = :id")
    int incrementViewCountByDelta(@Param("id") Long id, @Param("delta") int delta);

    // ========== 댓글 수 ==========

    // 댓글 수 증가 — Post 전체 조회 없이 UPDATE만 실행
    @Modifying
    @Query("UPDATE LostCatPost l " +
           "SET l.commentCount = l.commentCount + 1 " +
           "WHERE l.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    // 댓글 수 감소 — 0 미만으로 내려가지 않도록 보호
    @Modifying
    @Query("UPDATE LostCatPost l " +
           "SET l.commentCount = CASE WHEN l.commentCount > 0 THEN l.commentCount - 1 ELSE 0 END " +
           "WHERE l.id = :id")
    void decrementCommentCount(@Param("id") Long id);
}
