package com.min.meow.post.repository;

import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.QLostCatPostListResponse;
import com.min.meow.post.entity.QLostCatPost;
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

    // ngram_token_size(2) 미만 토큰은 FULLTEXT 인덱스에 없어 매치 불가
    private static final int MIN_TOKEN_LENGTH = 2;

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private final QLostCatPost lostCatPost = QLostCatPost.lostCatPost;

    @Override
    public Page<LostCatPostListResponse> findAllWithProjection(Pageable pageable) {
        List<LostCatPostListResponse> content = queryFactory
                .select(new QLostCatPostListResponse(
                        lostCatPost.id,
                        lostCatPost.title,
                        lostCatPost.catName,
                        lostCatPost.lostLocation,
                        lostCatPost.commentCount,
                        lostCatPost.view,
                        lostCatPost.isCompleted,
                        lostCatPost.createdAt,
                        lostCatPost.thumbnailUrl
                ))
                .from(lostCatPost)
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

    // BB 방식: 위도/경도 BETWEEN(B-Tree 인덱스) + ST_Distance_Sphere 정밀 필터 + 거리순 정렬
    @Override
    public Page<LostCatPostListResponse> findNearbyWithProjection(double lat, double lng, double radiusKm, Pageable pageable) {
        double radiusMeters = radiusKm * 1000;
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        String dataSql = """
                SELECT l.id, l.title, l.cat_name, l.lost_location,
                       l.comment_count, l.view, l.is_completed, l.created_at, l.thumbnail_url,
                       ST_Distance_Sphere(POINT(l.longitude, l.latitude), POINT(:lng, :lat)) AS distance
                FROM lost_cat_post l
                WHERE l.latitude IS NOT NULL AND l.longitude IS NOT NULL
                  AND l.latitude BETWEEN :latMin AND :latMax
                  AND l.longitude BETWEEN :lngMin AND :lngMax
                  AND ST_Distance_Sphere(POINT(l.longitude, l.latitude), POINT(:lng, :lat)) <= :radius
                ORDER BY distance
                LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM lost_cat_post l
                WHERE l.latitude IS NOT NULL AND l.longitude IS NOT NULL
                  AND l.latitude BETWEEN :latMin AND :latMax
                  AND l.longitude BETWEEN :lngMin AND :lngMax
                  AND ST_Distance_Sphere(POINT(l.longitude, l.latitude), POINT(:lng, :lat)) <= :radius
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
                        .catName((String) row[2])
                        .lostLocation((String) row[3])
                        .commentCount(((Number) row[4]).intValue())
                        .view(((Number) row[5]).intValue())
                        .completed(toBoolean(row[6]))
                        .createdAt(row[7] instanceof java.sql.Timestamp ts
                                ? ts.toLocalDateTime()
                                : (LocalDateTime) row[7])
                        .thumbnailUrl((String) row[8])
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

    // ST_Distance_Sphere 방식: SPATIAL INDEX(MBRContains) + ST 정밀필터 + 거리순 정렬
    @Override
    public Page<LostCatPostListResponse> findNearbyWithST(double lat, double lng, double radiusKm, Pageable pageable) {
        double radiusMeters = radiusKm * 1000;
        // MBRContains용 bbox 범위 계산 (SPATIAL INDEX 활용)
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        String dataSql = """
                SELECT l.id, l.title, l.cat_name, l.lost_location,
                       l.comment_count, l.view, l.is_completed, l.created_at, l.thumbnail_url,
                       ST_Distance_Sphere(l.location, ST_SRID(POINT(:lng, :lat), 4326)) AS distance
                FROM lost_cat_post l
                WHERE l.location IS NOT NULL
                  AND MBRContains(
                        ST_SRID(ST_MakeEnvelope(POINT(:lngMin, :latMin), POINT(:lngMax, :latMax)), 4326),
                        l.location)
                  AND ST_Distance_Sphere(l.location, ST_SRID(POINT(:lng, :lat), 4326)) <= :radius
                ORDER BY distance
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
                        .catName((String) row[2])
                        .lostLocation((String) row[3])
                        .commentCount(((Number) row[4]).intValue())
                        .view(((Number) row[5]).intValue())
                        .completed(toBoolean(row[6]))
                        .createdAt(row[7] instanceof java.sql.Timestamp ts
                                ? ts.toLocalDateTime()
                                : (LocalDateTime) row[7])
                        .thumbnailUrl((String) row[8])
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
                        lostCatPost.catName,
                        lostCatPost.lostLocation,
                        lostCatPost.commentCount,
                        lostCatPost.view,
                        lostCatPost.isCompleted,
                        lostCatPost.createdAt,
                        lostCatPost.thumbnailUrl
                ))
                .from(lostCatPost)
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
                SELECT l.id, l.title, l.cat_name, l.lost_location,
                       l.comment_count, l.view, l.is_completed, l.created_at, l.thumbnail_url
                FROM lost_cat_post l
                WHERE MATCH(l.title, l.contents, l.cat_name, l.lost_location) AGAINST(:keyword IN BOOLEAN MODE)
                  AND (:userId IS NULL OR l.user_id = :userId)
                ORDER BY l.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM lost_cat_post l
                WHERE MATCH(l.title, l.contents, l.cat_name, l.lost_location) AGAINST(:keyword IN BOOLEAN MODE)
                  AND (:userId IS NULL OR l.user_id = :userId)
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(dataSql)
                .setParameter("keyword", booleanKeyword)
                .setParameter("userId", userId)
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", (int) pageable.getOffset())
                .getResultList();

        // row 인덱스: id(0), title(1), catName(2), lostLocation(3),
        //             commentCount(4), view(5), isCompleted(6), createdAt(7), thumbnailUrl(8)
        List<LostCatPostListResponse> content = rows.stream()
                .map(row -> LostCatPostListResponse.builder()
                        .id(((Number) row[0]).longValue())
                        .title((String) row[1])
                        .catName((String) row[2])
                        .lostLocation((String) row[3])
                        .commentCount(((Number) row[4]).intValue())
                        .view(((Number) row[5]).intValue())
                        .completed(toBoolean(row[6]))
                        .createdAt(row[7] instanceof java.sql.Timestamp ts
                                ? ts.toLocalDateTime()
                                : (LocalDateTime) row[7])
                        .thumbnailUrl((String) row[8])
                        .build())
                .toList();

        long total = ((Number) entityManager.createNativeQuery(countSql)
                .setParameter("keyword", booleanKeyword)
                .setParameter("userId", userId)
                .getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<LostCatPostListResponse> findContentWithProjection(Pageable pageable) {
        return queryFactory
                .select(new QLostCatPostListResponse(
                        lostCatPost.id,
                        lostCatPost.title,
                        lostCatPost.catName,
                        lostCatPost.lostLocation,
                        lostCatPost.commentCount,
                        lostCatPost.view,
                        lostCatPost.isCompleted,
                        lostCatPost.createdAt,
                        lostCatPost.thumbnailUrl
                ))
                .from(lostCatPost)
                .orderBy(lostCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public long countAllPosts() {
        Long total = queryFactory
                .select(lostCatPost.count())
                .from(lostCatPost)
                .fetchOne();
        return total != null ? total : 0L;
    }

    /**
     * 커버링 인덱스 서브쿼리 + IN 절 방식
     * 커버링 인덱스로 id만 먼저 조회한 뒤, id 목록으로 IN 절 조회하여
     * LIMIT 개수만큼만 클러스터링 인덱스(PK)에 접근
     * IN 절은 순서를 보장하지 않으므로 본조회에도 ORDER BY를 다시 걸어야 함
     */
    @Override
    public List<LostCatPostListResponse> findContentWithCoveringIndexUsingIn(Pageable pageable) {
        List<Long> ids = queryFactory
                .select(lostCatPost.id)
                .from(lostCatPost)
                .orderBy(lostCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (ids.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(new QLostCatPostListResponse(
                        lostCatPost.id,
                        lostCatPost.title,
                        lostCatPost.catName,
                        lostCatPost.lostLocation,
                        lostCatPost.commentCount,
                        lostCatPost.view,
                        lostCatPost.isCompleted,
                        lostCatPost.createdAt,
                        lostCatPost.thumbnailUrl
                ))
                .from(lostCatPost)
                .where(lostCatPost.id.in(ids))
                .orderBy(lostCatPost.createdAt.desc())
                .fetch();
    }

    // 제목 OR 내용 LIKE 검색 조건
    // DB collation(utf8mb4_0900_ai_ci)이 이미 대소문자 구분 안 함 -> LOWER() 이중 적용 방지
    private BooleanExpression likeTitleOrContents(String title, String contents) {
        BooleanExpression titleExpr = (title != null && !title.isEmpty())
                ? lostCatPost.title.contains(title) : null;
        BooleanExpression contentsExpr = (contents != null && !contents.isEmpty())
                ? lostCatPost.contents.contains(contents) : null;
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
    // ngram_token_size(2) 미만 토큰은 인덱스에 없어 매치 불가하므로 제외
    private String sanitizeForBooleanMode(String keyword) {
        String cleaned = keyword.replaceAll("[+\\-><()~*\"@]", "");
        return Arrays.stream(cleaned.trim().split("\\s+"))
                .filter(token -> token.length() >= MIN_TOKEN_LENGTH)
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
