package com.min.meow.post.repository;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.QBoastCatPostListResponse;
import com.min.meow.post.entity.QBoastCatPost;
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

    private final QBoastCatPost boastCatPost = QBoastCatPost.boastCatPost;

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

    /**
     * 커버링 인덱스 서브쿼리 + IN 절 방식
     * 기존: SELECT 전체컬럼 FROM boast_cat_post ORDER BY created_at DESC LIMIT 10 OFFSET 10000
     *   → OFFSET 10000이면 10000개 행을 모두 읽고 버림 (느림)
     * 개선: 커버링 인덱스로 id만 먼저 조회한 뒤, id 목록으로 IN 절 조회하여
     *   LIMIT 개수만큼만 클러스터링 인덱스(PK)에 접근
     * IN 절은 순서를 보장하지 않으므로 본조회에도 ORDER BY를 다시 걸어야 함
     */
    @Override
    public List<BoastCatPostListResponse> findContentWithCoveringIndexUsingIn(Pageable pageable) {
        List<Long> ids = queryFactory
                .select(boastCatPost.id)
                .from(boastCatPost)
                .orderBy(boastCatPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (ids.isEmpty()) {
            return List.of();
        }

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
                .where(boastCatPost.id.in(ids))
                .orderBy(boastCatPost.createdAt.desc())
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
}
