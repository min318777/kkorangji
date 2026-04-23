package com.min.meow.post.repository;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.QBoastCatPostListResponse;
import com.min.meow.post.entity.QBoastCatPost;
import com.min.meow.user.entity.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class BoastCatPostRepositoryImpl implements BoastCatPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // Q클래스 인스턴스 (QueryDSL 타입 안전 쿼리용)
    private final QBoastCatPost boastCatPost = QBoastCatPost.boastCatPost;
    private final QUser user = QUser.user;

    /**
     * 최근 게시물 20개 Projection 조회
     *
     * 성능 최적화 포인트:
     * 1. SELECT 절에 필요한 컬럼만 명시 (contents 등 불필요한 컬럼 제외)
     * 2. Entity 변환 없이 DTO로 직접 매핑 (영속성 컨텍스트 오버헤드 제거)
     * 3. User 테이블에서 loginId만 조회 (User 전체 로딩 X)
     *
     * 실행되는 쿼리:
     * SELECT b.id, b.title, u.login_id, b.like_count, b.comment_count, b.view, b.created_at
     * FROM boast_cat_post b
     * LEFT JOIN users u ON b.user_id = u.id
     * ORDER BY b.created_at DESC
     * LIMIT 20
     *
     * BoastCatPostListResponse를 재사용하여 코드 중복 제거
     */
    @Override
    public List<BoastCatPostListResponse> findTop20RecentWithProjection() {
        return queryFactory
                .select(new QBoastCatPostListResponse(
                        boastCatPost.id,
                        boastCatPost.title,
                        boastCatPost.user.loginId,
                        boastCatPost.likeCount,
                        boastCatPost.commentCount,
                        boastCatPost.view,
                        boastCatPost.createdAt,
                        boastCatPost.thumbnailUrl
                ))
                .from(boastCatPost)
                .leftJoin(boastCatPost.user, user)  // FETCH가 아닌 일반 JOIN
                .orderBy(boastCatPost.createdAt.desc())
                .limit(20)
                .fetch();
    }

    /**
     * 게시글 검색 (Projection 적용)
     * - selectFrom(Entity 전체) → select(필요한 7개 컬럼만)으로 변경
     * - contents(TEXT), imageUrls, comments 등 불필요한 컬럼 제거
     * - leftJoin으로 User.loginId만 조회
     */
    @Override
    public Page<BoastCatPostListResponse> search(String title, String contents, Long userId, Pageable pageable) {
        List<BoastCatPostListResponse> results = queryFactory
                .select(new QBoastCatPostListResponse(
                        boastCatPost.id,
                        boastCatPost.title,
                        boastCatPost.user.loginId,
                        boastCatPost.likeCount,
                        boastCatPost.commentCount,
                        boastCatPost.view,
                        boastCatPost.createdAt,
                        boastCatPost.thumbnailUrl
                ))
                .from(boastCatPost)
                .leftJoin(boastCatPost.user, user)
                .where(
                        likeTitle(title),
                        likeContents(contents),
                        eqUserId(userId))
                .orderBy(boastCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(boastCatPost.count())
                .from(boastCatPost)
                .where(
                        likeTitle(title),
                        likeContents(contents),
                        eqUserId(userId))
                .fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0L);
    }

    /**
     * 제목 LIKE 검색 조건
     * LIKE '%keyword%' 방식으로 부분 문자열 검색
     */
    private BooleanExpression likeTitle(String title) {
        if (title == null || title.isEmpty()) return null;
        return boastCatPost.title.containsIgnoreCase(title);
    }

    /**
     * 내용 LIKE 검색 조건
     * LIKE '%keyword%' 방식으로 부분 문자열 검색
     */
    private BooleanExpression likeContents(String contents) {
        if (contents == null || contents.isEmpty()) return null;
        return boastCatPost.contents.containsIgnoreCase(contents);
    }

    // userId 일치 조건 (null이면 전체 검색)
    private BooleanExpression eqUserId(Long userId) {
        if (userId == null) return null;
        return boastCatPost.user.id.eq(userId);
    }

    /**
     * 게시글 목록 페이징 조회 (Projection 적용)
     * 성능 최적화 포인트:
     * 1. SELECT 절에 목록에 필요한 컬럼만 명시 (contents, imageUrls, comments 제외)
     * 2. Entity 변환 없이 DTO로 직접 매핑 (영속성 컨텍스트 오버헤드 제거)
     * 3. User 테이블에서 loginId만 조회 (User 전체 로딩 X)
     * 실행되는 쿼리:
     * SELECT b.id, b.title, u.login_id, b.like_count, b.comment_count, b.view, b.created_at
     * FROM boast_cat_post b
     * LEFT JOIN users u ON b.user_id = u.id
     * ORDER BY b.created_at DESC
     * LIMIT ? OFFSET ?
     * 기존 방식 대비 개선 효과:
     * - contents (TEXT) 컬럼 조회 제거 → 대용량 텍스트 전송 방지
     * - imageUrls, comments 연관 테이블 조회 제거 → 추가 쿼리 방지
     */
    @Override
    public Page<BoastCatPostListResponse> findAllWithProjection(Pageable pageable) {
        // 목록 데이터 조회 (Projection으로 필요한 컬럼만 SELECT)
        List<BoastCatPostListResponse> content = queryFactory
                .select(new QBoastCatPostListResponse(
                        boastCatPost.id,
                        boastCatPost.title,
                        boastCatPost.user.loginId,
                        boastCatPost.likeCount,
                        boastCatPost.commentCount,
                        boastCatPost.view,
                        boastCatPost.createdAt,
                        boastCatPost.thumbnailUrl
                ))
                .from(boastCatPost)
                .leftJoin(boastCatPost.user, user)
                .orderBy(boastCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 조회 (페이징 정보용)
        Long total = queryFactory
                .select(boastCatPost.count())
                .from(boastCatPost)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
