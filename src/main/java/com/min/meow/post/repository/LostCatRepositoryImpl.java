package com.min.meow.post.repository;

import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.QLostCatPostListResponse;
import com.min.meow.post.entity.QLostCatPost;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class LostCatRepositoryImpl implements LostCatRepositoryCustom {

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

    // BIT(1) → boolean 변환 (드라이버별 반환 타입 방어 처리)
    private boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        if (value instanceof byte[] arr) return arr.length > 0 && arr[0] != 0;
        return false;
    }
}
