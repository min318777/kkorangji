package com.min.meow.search.repository;

import com.min.meow.post.dto.response.LostCatPostListResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Repository
public class LostCatPostSearchRepositoryImpl implements LostCatPostSearchRepositoryCustom {

    // ngram_token_size(2) 미만 토큰은 FULLTEXT 인덱스에 없어 매치 불가
    private static final int MIN_TOKEN_LENGTH = 2;

    @PersistenceContext
    private EntityManager entityManager;

    // FTS 검색: MATCH AGAINST (ngram 파서)
    @Override
    public Page<LostCatPostListResponse> searchByKeyword(String keyword, Pageable pageable) {
        // BOOLEAN MODE: 고빈도 단어 50% 규칙 없음, 단순 포함 여부만 체크
        String booleanKeyword = sanitizeForBooleanMode(keyword);

        String dataSql = """
                SELECT l.id, l.title, l.cat_name, l.lost_location,
                       l.comment_count, l.view, l.is_completed, l.created_at, l.thumbnail_url
                FROM lost_cat_post l
                WHERE MATCH(l.title, l.contents, l.cat_name, l.lost_location) AGAINST(:keyword IN BOOLEAN MODE)
                ORDER BY l.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM lost_cat_post l
                WHERE MATCH(l.title, l.contents, l.cat_name, l.lost_location) AGAINST(:keyword IN BOOLEAN MODE)
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(dataSql)
                .setParameter("keyword", booleanKeyword)
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
                .getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
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
