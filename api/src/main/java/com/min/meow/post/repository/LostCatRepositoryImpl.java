package com.min.meow.post.repository;

import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.QLostCatPostListResponse;
import com.min.meow.post.entity.QLostCatPost;
import com.min.meow.user.entity.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 실종 고양이 게시글 커스텀 리포지토리 구현체
 *
 * QueryDSL을 사용하여 DTO Projection으로 성능 최적화
 * - Entity 대신 DTO를 직접 조회하여 LazyInitializationException 방지
 * - 필요한 컬럼만 SELECT하여 네트워크 트래픽 감소
 */
@RequiredArgsConstructor
@Repository
public class LostCatRepositoryImpl implements LostCatRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // Q클래스 인스턴스 (QueryDSL 타입 안전 쿼리용)
    private final QLostCatPost lostCatPost = QLostCatPost.lostCatPost;
    private final QUser user = QUser.user;

    /**
     * 게시글 목록 페이징 조회 (Projection 적용)
     *
     * 성능 최적화 포인트:
     * 1. SELECT 절에 목록에 필요한 컬럼만 명시 (contents, imageUrls, comments 제외)
     * 2. Entity 변환 없이 DTO로 직접 매핑 (영속성 컨텍스트 오버헤드 제거)
     * 3. User 테이블에서 loginId만 조회 (User 전체 로딩 X)
     *
     * 실행되는 쿼리:
     * SELECT l.id, l.title, u.login_id, l.cat_name, l.lost_location,
     *        l.comment_count, l.view, l.is_completed, l.created_at
     * FROM lost_cat_post l
     * LEFT JOIN users u ON l.user_id = u.id
     * ORDER BY l.created_at DESC
     * LIMIT ? OFFSET ?
     *
     * 기존 방식 대비 개선 효과:
     * - contents (TEXT) 컬럼 조회 제거 → 대용량 텍스트 전송 방지
     * - imageUrls, comments 연관 테이블 조회 제거 → 추가 쿼리 방지
     * - LazyInitializationException 완전 방지
     */
    @Override
    public Page<LostCatPostListResponse> findAllWithProjection(Pageable pageable) {
        // 목록 데이터 조회 (Projection으로 필요한 컬럼만 SELECT)
        List<LostCatPostListResponse> content = queryFactory
                .select(new QLostCatPostListResponse(
                        lostCatPost.id,
                        lostCatPost.title,
                        lostCatPost.user.loginId,       // User에서 loginId만 조회
                        lostCatPost.catName,
                        lostCatPost.lostLocation,
                        lostCatPost.commentCount,
                        lostCatPost.view,
                        lostCatPost.isCompleted,
                        lostCatPost.createdAt
                ))
                .from(lostCatPost)
                .leftJoin(lostCatPost.user, user)       // FETCH가 아닌 일반 JOIN
                .orderBy(lostCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 조회 (페이징 정보용)
        Long total = queryFactory
                .select(lostCatPost.count())
                .from(lostCatPost)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 최근 게시물 20개 Projection 조회
     *
     * 성능 최적화 포인트:
     * 1. SELECT 절에 목록에 필요한 컬럼만 명시 (contents, imageUrls 등 제외)
     * 2. Entity 변환 없이 DTO로 직접 매핑 (영속성 컨텍스트 오버헤드 제거)
     * 3. User 테이블에서 loginId만 조회 (User 전체 로딩 X)
     *
     * 실행되는 쿼리:
     * SELECT l.id, l.title, u.login_id, l.cat_name, l.lost_location,
     *        l.comment_count, l.view, l.is_completed, l.created_at
     * FROM lost_cat_post l
     * LEFT JOIN users u ON l.user_id = u.id
     * ORDER BY l.created_at DESC
     * LIMIT 20
     *
     * 기존 방식 대비 개선 효과:
     * - contents (TEXT) 컬럼 조회 제거 → 대용량 텍스트 전송 방지
     * - imageUrls 연관 테이블 조회 제거 → 추가 쿼리 방지
     * - LostCatPostListResponse 재사용으로 코드 중복 제거
     */
    @Override
    public List<LostCatPostListResponse> findTop20RecentWithProjection() {
        return queryFactory
                .select(new QLostCatPostListResponse(
                        lostCatPost.id,
                        lostCatPost.title,
                        lostCatPost.user.loginId,       // User에서 loginId만 조회
                        lostCatPost.catName,
                        lostCatPost.lostLocation,
                        lostCatPost.commentCount,
                        lostCatPost.view,
                        lostCatPost.isCompleted,
                        lostCatPost.createdAt
                ))
                .from(lostCatPost)
                .leftJoin(lostCatPost.user, user)       // FETCH가 아닌 일반 JOIN
                .orderBy(lostCatPost.createdAt.desc())
                .limit(20)
                .fetch();
    }
}
