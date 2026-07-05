package com.min.meow.post.repository;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.QBoastCatPostListResponse;
import com.min.meow.post.entity.QBoastCatPost;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class BoastCatPostRepositoryImpl implements BoastCatPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // Native Query 실행용 (MATCH AGAINST는 QueryDSL 미지원)
    @PersistenceContext
    private EntityManager entityManager;

    private final QBoastCatPost boastCatPost = QBoastCatPost.boastCatPost;

    /**
     * LIKE 검색 (성능 비교 기준선)
     * - LIKE '%keyword%' 방식 → 인덱스 미사용, Full Table Scan
     */
    @Override
    public Page<BoastCatPostListResponse> search(String title, String contents, Long userId, Pageable pageable) {
        List<BoastCatPostListResponse> results = queryFactory
                .select(new QBoastCatPostListResponse(
                        boastCatPost.id,
                        boastCatPost.title,
                        boastCatPost.likeCount,
                        boastCatPost.commentCount,
                        boastCatPost.view,
                        boastCatPost.createdAt,
                        boastCatPost.thumbnailUrl
                ))
                .from(boastCatPost)
                .where(
                        likeTitleOrContents(title, contents),
                        eqUserId(userId))
                .orderBy(boastCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(boastCatPost.count())
                .from(boastCatPost)
                .where(
                        likeTitleOrContents(title, contents),
                        eqUserId(userId))
                .fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0L);
    }

    /**
     * Full-Text Search (ngram 파서)
     * - MATCH(title, contents) AGAINST(keyword IN BOOLEAN MODE)
     * - FULLTEXT INDEX ft_boast_post_title_contents 활용
     * - 한국어 2-gram 토큰화로 인덱스 기반 검색
     */
    @Override
    public Page<BoastCatPostListResponse> searchByKeyword(String keyword, Long userId, Pageable pageable) {
        // BOOLEAN MODE: 고빈도 단어 50% 규칙 없음, 단순 포함 여부만 체크
        String booleanKeyword = sanitizeForBooleanMode(keyword);

        String dataSql = """
                SELECT b.id, b.title, b.like_count, b.comment_count,
                       b.view, b.created_at, b.thumbnail_url
                FROM boast_cat_post b
                WHERE MATCH(b.title, b.contents) AGAINST(:keyword IN BOOLEAN MODE)
                  AND (:userId IS NULL OR b.user_id = :userId)
                ORDER BY b.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM boast_cat_post b
                WHERE MATCH(b.title, b.contents) AGAINST(:keyword IN BOOLEAN MODE)
                  AND (:userId IS NULL OR b.user_id = :userId)
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(dataSql)
                .setParameter("keyword", booleanKeyword)
                .setParameter("userId", userId)
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", (int) pageable.getOffset())
                .getResultList();

        // row 인덱스: id(0), title(1), like_count(2), comment_count(3), view(4), created_at(5), thumbnail_url(6)
        List<BoastCatPostListResponse> content = rows.stream()
                .map(row -> BoastCatPostListResponse.builder()
                        .id(((Number) row[0]).longValue())
                        .title((String) row[1])
                        .likeCount(((Number) row[2]).intValue())
                        .commentCount(((Number) row[3]).intValue())
                        .view(((Number) row[4]).intValue())
                        .createdAt(row[5] instanceof java.sql.Timestamp ts
                                ? ts.toLocalDateTime()
                                : (LocalDateTime) row[5])
                        .thumbnailUrl((String) row[6])
                        .build())
                .toList();

        long total = ((Number) entityManager.createNativeQuery(countSql)
                .setParameter("keyword", booleanKeyword)
                .setParameter("userId", userId)
                .getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<BoastCatPostListResponse> findAllWithProjection(Pageable pageable) {
        List<BoastCatPostListResponse> content = findContentWithProjection(pageable);
        long total = countAllPosts();
        return new PageImpl<>(content, pageable, total);
    }

    // content만 조회 (COUNT 쿼리 없음) - 캐싱된 count와 조합하여 사용
    @Override
    public List<BoastCatPostListResponse> findContentWithProjection(Pageable pageable) {
        return queryFactory
                .select(new QBoastCatPostListResponse(
                        boastCatPost.id,
                        boastCatPost.title,
                        boastCatPost.likeCount,
                        boastCatPost.commentCount,
                        boastCatPost.view,
                        boastCatPost.createdAt,
                        boastCatPost.thumbnailUrl
                ))
                .from(boastCatPost)
                .orderBy(boastCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    // 전체 게시글 수만 조회 - BoastCatPostCountCacheService에서 캐싱
    @Override
    public long countAllPosts() {
        Long total = queryFactory
                .select(boastCatPost.count())
                .from(boastCatPost)
                .fetchOne();
        return total != null ? total : 0L;
    }

    // 제목 OR 내용 LIKE 검색 조건 (하나라도 포함하면 매칭)
    private BooleanExpression likeTitleOrContents(String title, String contents) {
        BooleanExpression titleExpr = (title != null && !title.isEmpty())
                ? boastCatPost.title.containsIgnoreCase(title) : null;
        BooleanExpression contentsExpr = (contents != null && !contents.isEmpty())
                ? boastCatPost.contents.containsIgnoreCase(contents) : null;
        if (titleExpr == null) return contentsExpr;
        if (contentsExpr == null) return titleExpr;
        return titleExpr.or(contentsExpr);
    }

    // userId 일치 조건 (null이면 전체 검색)
    private BooleanExpression eqUserId(Long userId) {
        if (userId == null) return null;
        return boastCatPost.user.id.eq(userId);
    }

    // BOOLEAN MODE 변환: 각 단어를 + (필수 조건)으로 처리
    // "고양이 귀여운" → "+고양이 +귀여운"
    // BOOLEAN MODE 특수문자(+,-,*,~,",(,)) 제거 후 각 토큰에 + 붙임
    private String sanitizeForBooleanMode(String keyword) {
        String cleaned = keyword.replaceAll("[+\\-><()~*\"@]", "");
        return Arrays.stream(cleaned.trim().split("\\s+"))
                .filter(token -> !token.isEmpty())
                .map(token -> "+" + token)
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
