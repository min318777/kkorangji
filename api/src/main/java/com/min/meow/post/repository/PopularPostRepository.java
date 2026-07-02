package com.min.meow.post.repository;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.QBoastCatPostListResponse;
import com.min.meow.post.entity.QBoastCatPost;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 인기글 전용 Repository
 * BoastCatPostRepositoryCustom에서 분리 — 인기글 조회/랭킹 초기화 전용
 */
@Repository
@RequiredArgsConstructor
public class PopularPostRepository {

    private final JPAQueryFactory queryFactory;
    private final QBoastCatPost boastCatPost = QBoastCatPost.boastCatPost;

    /**
     * 가중치 점수(좋아요×3 + 댓글×2 + 조회수×1) 기준 TOP 24 인기 게시물 조회
     * Sorted Set이 비어있을 때 fallback으로 사용
     */
    public List<BoastCatPostListResponse> findTop24ByScore() {
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
                .orderBy(
                        boastCatPost.likeCount.multiply(3)
                                .add(boastCatPost.commentCount.multiply(2))
                                .add(boastCatPost.view)
                                .desc()
                )
                .limit(24)
                .fetch();
    }

    /**
     * ID 목록으로 게시글 조회 — Sorted Set v5에서 ID 확보 후 데이터 조회 시 사용
     */
    public List<BoastCatPostListResponse> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
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
                .fetch();
    }

    /**
     * 랭킹 초기화용 전체 게시글 (id, likeCount, commentCount, view) 조회
     * @PostConstruct에서 Sorted Set 초기값 계산에 사용
     */
    public List<Tuple> findAllForRankingInit() {
        return queryFactory
                .select(boastCatPost.id, boastCatPost.likeCount, boastCatPost.commentCount, boastCatPost.view)
                .from(boastCatPost)
                .fetch();
    }
}
