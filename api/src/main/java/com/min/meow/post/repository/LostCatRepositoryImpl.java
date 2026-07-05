package com.min.meow.post.repository;

import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.QLostCatPostListResponse;
import com.min.meow.post.entity.QLostCatPost;
import com.min.meow.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
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
public class LostCatRepositoryImpl implements LostCatRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private final QLostCatPost lostCatPost = QLostCatPost.lostCatPost;
    private final QUser user = QUser.user;

    @Override
    public Page<LostCatPostListResponse> findAllWithProjection(Pageable pageable) {
        List<LostCatPostListResponse> content = queryFactory
                .select(new QLostCatPostListResponse(
                        lostCatPost.id,
                        lostCatPost.title,
                        lostCatPost.user.loginId,
                        lostCatPost.catName,
                        lostCatPost.lostLocation,
                        lostCatPost.commentCount,
                        lostCatPost.view,
                        lostCatPost.isCompleted,
                        lostCatPost.createdAt,
                        lostCatPost.thumbnailUrl
                ))
                .from(lostCatPost)
                .leftJoin(lostCatPost.user, user)
                .orderBy(lostCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(lostCatPost.count())
                .from(lostCatPost)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<LostCatPostListResponse> findNearbyWithProjection(double lat, double lng, double radiusKm, Pageable pageable) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        BooleanBuilder where = new BooleanBuilder();
        where.and(lostCatPost.latitude.isNotNull());
        where.and(lostCatPost.longitude.isNotNull());
        where.and(lostCatPost.latitude.between(lat - latDelta, lat + latDelta));
        where.and(lostCatPost.longitude.between(lng - lngDelta, lng + lngDelta));

        List<LostCatPostListResponse> content = queryFactory
                .select(new QLostCatPostListResponse(
                        lostCatPost.id,
                        lostCatPost.title,
                        lostCatPost.user.loginId,
                        lostCatPost.catName,
                        lostCatPost.lostLocation,
                        lostCatPost.commentCount,
                        lostCatPost.view,
                        lostCatPost.isCompleted,
                        lostCatPost.createdAt,
                        lostCatPost.thumbnailUrl
                ))
                .from(lostCatPost)
                .leftJoin(lostCatPost.user, user)
                .where(where)
                .orderBy(lostCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(lostCatPost.count())
                .from(lostCatPost)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // ST_Distance_Sphere 방식: SPATIAL INDEX(MBRContains) + ST 정밀필터 + 거리순 정렬
    @Override
    public Page<LostCatPostListResponse> findNearbyWithST(double lat, double lng, double radiusKm, Pageable pageable) {
        double radiusMeters = radiusKm * 1000;
        // MBRContains용 bbox 범위 계산 (SPATIAL INDEX 활용)
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        String dataSql = """
                SELECT l.id, l.title, u.login_id, l.cat_name, l.lost_location,
                       l.comment_count, l.view, l.is_completed, l.created_at, l.thumbnail_url
                FROM lost_cat_post l
                INNER JOIN user u ON u.id = l.user_id
                WHERE l.location IS NOT NULL
                  AND MBRContains(
                        ST_SRID(ST_MakeEnvelope(POINT(:lngMin, :latMin), POINT(:lngMax, :latMax)), 4326),
                        l.location)
                  AND ST_Distance_Sphere(l.location, ST_SRID(POINT(:lng, :lat), 4326)) <= :radius
                ORDER BY ST_Distance_Sphere(l.location, ST_SRID(POINT(:lng, :lat), 4326))
                LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM lost_cat_post l
                WHERE l.location IS NOT NULL
                  AND MBRContains(
                        ST_SRID(ST_MakeEnvelope(POINT(:lngMin, :latMin), POINT(:lngMax, :latMax)), 4326),
                        l.location)
                  AND ST_Distance_Sphere(l.location, ST_SRID(POINT(:lng, :lat), 4326)) <= :radius
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(dataSql)
                .setParameter("lat", lat)
                .setParameter("lng", lng)
                .setParameter("latMin", lat - latDelta)
                .setParameter("latMax", lat + latDelta)
                .setParameter("lngMin", lng - lngDelta)
                .setParameter("lngMax", lng + lngDelta)
                .setParameter("radius", radiusMeters)
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", (int) pageable.getOffset())
                .getResultList();

        List<LostCatPostListResponse> content = rows.stream()
                .map(row -> LostCatPostListResponse.builder()
                        .id(((Number) row[0]).longValue())
                        .title((String) row[1])
                        .writer((String) row[2])
                        .catName((String) row[3])
                        .lostLocation((String) row[4])
                        .commentCount(((Number) row[5]).intValue())
                        .view(((Number) row[6]).intValue())
                        .completed(toBoolean(row[7]))
                        .createdAt(row[8] instanceof java.sql.Timestamp ts
                                ? ts.toLocalDateTime()
                                : (LocalDateTime) row[8])
                        .thumbnailUrl((String) row[9])
                        .build())
                .toList();

        long total = ((Number) entityManager.createNativeQuery(countSql)
                .setParameter("lat", lat)
                .setParameter("lng", lng)
                .setParameter("latMin", lat - latDelta)
                .setParameter("latMax", lat + latDelta)
                .setParameter("lngMin", lng - lngDelta)
                .setParameter("lngMax", lng + lngDelta)
                .setParameter("radius", radiusMeters)
                .getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    // LIKE 검색: '%keyword%' 방식
    @Override
    public Page<LostCatPostListResponse> search(String title, String contents, Long userId, Pageable pageable) {
        List<LostCatPostListResponse> results = queryFactory
                .select(new QLostCatPostListResponse(
                        lostCatPost.id,
                        lostCatPost.title,
                        lostCatPost.user.loginId,
                        lostCatPost.catName,
                        lostCatPost.lostLocation,
                        lostCatPost.commentCount,
                        lostCatPost.view,
                        lostCatPost.isCompleted,
                        lostCatPost.createdAt,
                        lostCatPost.thumbnailUrl
                ))
                .from(lostCatPost)
                .leftJoin(lostCatPost.user, user)
                .where(
                        likeTitleOrContents(title, contents),
                        eqUserId(userId))
                .orderBy(lostCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(lostCatPost.count())
                .from(lostCatPost)
                .where(
                        likeTitleOrContents(title, contents),
                        eqUserId(userId))
                .fetchOne();

        return new PageImpl<>(results, pageable, total != null ? total : 0L);
    }

    // FTS 검색: MATCH AGAINST (ngram 파서)
    @Override
    public Page<LostCatPostListResponse> searchByKeyword(String keyword, Long userId, Pageable pageable) {
        // BOOLEAN MODE: 고빈도 단어 50% 규칙 없음, 단순 포함 여부만 체크
        String booleanKeyword = sanitizeForBooleanMode(keyword);

        String dataSql = """
                SELECT l.id, l.title, u.login_id, l.cat_name, l.lost_location,
                       l.comment_count, l.view, l.is_completed, l.created_at, l.thumbnail_url
                FROM lost_cat_post l
                INNER JOIN user u ON u.id = l.user_id
                WHERE MATCH(l.title, l.contents) AGAINST(:keyword IN BOOLEAN MODE)
                  AND (:userId IS NULL OR l.user_id = :userId)
                ORDER BY l.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM lost_cat_post l
                WHERE MATCH(l.title, l.contents) AGAINST(:keyword IN BOOLEAN MODE)
                  AND (:userId IS NULL OR l.user_id = :userId)
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(dataSql)
                .setParameter("keyword", booleanKeyword)
                .setParameter("userId", userId)
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", (int) pageable.getOffset())
                .getResultList();

        // row 인덱스: id(0), title(1), writer(2), catName(3), lostLocation(4),
        //             commentCount(5), view(6), isCompleted(7), createdAt(8), thumbnailUrl(9)
        List<LostCatPostListResponse> content = rows.stream()
                .map(row -> LostCatPostListResponse.builder()
                        .id(((Number) row[0]).longValue())
                        .title((String) row[1])
                        .writer((String) row[2])
                        .catName((String) row[3])
                        .lostLocation((String) row[4])
                        .commentCount(((Number) row[5]).intValue())
                        .view(((Number) row[6]).intValue())
                        .completed(toBoolean(row[7]))
                        .createdAt(row[8] instanceof java.sql.Timestamp ts
                                ? ts.toLocalDateTime()
                                : (LocalDateTime) row[8])
                        .thumbnailUrl((String) row[9])
                        .build())
                .toList();

        long total = ((Number) entityManager.createNativeQuery(countSql)
                .setParameter("keyword", booleanKeyword)
                .setParameter("userId", userId)
                .getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    // 제목 OR 내용 LIKE 검색 조건
    private BooleanExpression likeTitleOrContents(String title, String contents) {
        BooleanExpression titleExpr = (title != null && !title.isEmpty())
                ? lostCatPost.title.containsIgnoreCase(title) : null;
        BooleanExpression contentsExpr = (contents != null && !contents.isEmpty())
                ? lostCatPost.contents.containsIgnoreCase(contents) : null;
        if (titleExpr == null) return contentsExpr;
        if (contentsExpr == null) return titleExpr;
        return titleExpr.or(contentsExpr);
    }

    // userId 일치 조건 (null이면 전체 검색)
    private BooleanExpression eqUserId(Long userId) {
        if (userId == null) return null;
        return lostCatPost.user.id.eq(userId);
    }

    // BOOLEAN MODE 변환: 각 단어를 + (필수 조건)으로 처리
    private String sanitizeForBooleanMode(String keyword) {
        String cleaned = keyword.replaceAll("[+\\-><()~*\"@]", "");
        return Arrays.stream(cleaned.trim().split("\\s+"))
                .filter(token -> !token.isEmpty())
                .map(token -> "+" + token)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    // BIT(1) → boolean 변환 (드라이버별 반환 타입 방어 처리)
    private boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        if (value instanceof byte[] arr) return arr.length > 0 && arr[0] != 0;
        return false;
    }
}
