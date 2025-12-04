package com.min.meow.post.repository;

import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.QBoastCatPost;
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

    QBoastCatPost boastCatPost = QBoastCatPost.boastCatPost;

    @Override
    public Page<BoastCatPost> search(String title, String contents, Pageable pageable) {
        List<BoastCatPost> results = queryFactory
                .selectFrom(boastCatPost)
                .where(
                        containsTitle(title),
                        containsContents(contents)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(boastCatPost.createdAt.desc())
                .fetch();

        long total = queryFactory
                .select(boastCatPost.count())
                .from(boastCatPost)
                .where(
                        containsTitle(title),
                        containsContents(contents)
                )
                .fetchOne();

        return new PageImpl<>(results, pageable, total);
    }

    // 제목 검색 조건
    private BooleanExpression containsTitle(String title) {
        return title != null && !title.isEmpty() ? boastCatPost.title.contains(title) : null;
    }

    // 내용 검색 조건
    private BooleanExpression containsContents(String contents) {
        return contents != null && !contents.isEmpty() ? boastCatPost.contents.contains(contents) : null;
    }
}