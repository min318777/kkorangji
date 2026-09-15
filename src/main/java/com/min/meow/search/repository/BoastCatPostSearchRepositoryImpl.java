package com.min.meow.search.repository;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
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
public class BoastCatPostSearchRepositoryImpl implements BoastCatPostSearchRepositoryCustom {

    // ngram_token_size(2) 미만 토큰은 FULLTEXT 인덱스에 없어 매치 불가
    private static final int MIN_TOKEN_LENGTH = 2;

    // Native Query 실행용 (MATCH AGAINST는 QueryDSL 미지원)
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Full-Text Search (ngram 파서)
     * - MATCH(title, contents) AGAINST(keyword IN BOOLEAN MODE)
     * - FULLTEXT INDEX ft_boast_post_title_contents 활용
     * - 한국어 2-gram 토큰화로 인덱스 기반 검색
     * - 관련도 점수(MATCH...AGAINST) 순 정렬 -> FULLTEXT 인덱스가 이미 관련도순으로 내놓는 결과를
     *   그대로 소비해 상위 N개만 가져옴 (매치 건수와 무관하게 응답시간 일정, EXPLAIN에서 filesort 제거 확인됨)
     */
    @Override
    public Page<BoastCatPostListResponse> searchByKeyword(String keyword, Pageable pageable) {
        // BOOLEAN MODE: 고빈도 단어 50% 규칙 없음, 단순 포함 여부만 체크
        String booleanKeyword = sanitizeForBooleanMode(keyword);

        String dataSql = """
                SELECT b.id, b.title, b.like_count, b.comment_count,
                       b.view, b.created_at, b.thumbnail_url
                FROM boast_cat_post b
                WHERE MATCH(b.title, b.contents) AGAINST(:keyword IN BOOLEAN MODE)
                ORDER BY MATCH(b.title, b.contents) AGAINST(:keyword IN BOOLEAN MODE) DESC
                LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM boast_cat_post b
                WHERE MATCH(b.title, b.contents) AGAINST(:keyword IN BOOLEAN MODE)
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(dataSql)
                .setParameter("keyword", booleanKeyword)
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
                .getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Full-Text Search (자연어 모드)
     * - MATCH(title, contents) AGAINST(keyword IN NATURAL LANGUAGE MODE)
     * - 연산자(+/-) 문법이 없어 키워드를 가공 없이 그대로 전달, 암묵적 OR + 관련도 점수
     * - 흔한 단어 자동 제외(50% 임계치) 규칙은 MyISAM 전용이며 이 테이블(InnoDB)에는 적용되지 않음
     */
    @Override
    public Page<BoastCatPostListResponse> searchByNaturalLanguage(String keyword, Pageable pageable) {
        String dataSql = """
                SELECT b.id, b.title, b.like_count, b.comment_count,
                       b.view, b.created_at, b.thumbnail_url
                FROM boast_cat_post b
                WHERE MATCH(b.title, b.contents) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                ORDER BY b.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM boast_cat_post b
                WHERE MATCH(b.title, b.contents) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(dataSql)
                .setParameter("keyword", keyword)
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
                .setParameter("keyword", keyword)
                .getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    // BOOLEAN MODE 변환: 각 단어를 + (필수 조건)으로 처리
    // "고양이 귀여운" → "+고양이 +귀여운"
    // BOOLEAN MODE 특수문자(+,-,*,~,",(,)) 제거 후 각 토큰에 + 붙임
    // ngram_token_size(2) 미만 토큰은 인덱스에 없어 매치 불가하므로 제외
    private String sanitizeForBooleanMode(String keyword) {
        String cleaned = keyword.replaceAll("[+\\-><()~*\"@]", "");
        return Arrays.stream(cleaned.trim().split("\\s+"))
                .filter(token -> token.length() >= MIN_TOKEN_LENGTH)
                .map(token -> "+" + token)
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
