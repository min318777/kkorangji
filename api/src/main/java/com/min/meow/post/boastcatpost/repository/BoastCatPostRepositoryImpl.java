package com.min.meow.post.boastcatpost.repository;


import com.min.meow.post.boastcatpost.entity.BoastCatPost;
import com.min.meow.post.boastcatpost.entity.QBoastCatPost;
import com.min.meow.post.search.domain.request.PostSearchRequest;
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


    /*
    실무에서 흔히 쓰는 검색 조건 예시
    키워드 검색 (제목, 내용 포함)
    작성자명 검색 (닉네임, 이름)
    카테고리 / 태그 필터링
    날짜 범위 검색 (작성일, 수정일)
    정렬 조건 (최신순, 조회순 등)


     */
    @Override
    public Page<BoastCatPost> search(PostSearchRequest postSearchRequest, Pageable pageable) {
        List<BoastCatPost> results = queryFactory
                .selectFrom(boastCatPost)
                .where(
                        containsId(postSearchRequest.getId()),
                        containsTitle(postSearchRequest.getTitle()),
                        containsContents(postSearchRequest.getContents())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(boastCatPost.createdAt.desc())
                .fetch();

        long total = queryFactory
                .select(boastCatPost.count())
                .from(boastCatPost)
                .where(
                        containsId(postSearchRequest.getId()),
                        containsTitle(postSearchRequest.getTitle()),
                        containsContents(postSearchRequest.getContents())
                )
                .fetchOne();

        return new PageImpl<>(results, pageable, total);
    }

    private BooleanExpression containsId(Long id){
        return id == null ? null : boastCatPost.id.eq(id);
    }

    private BooleanExpression containsTitle(String title){
        return (title == null || title.isBlank()) ? null : boastCatPost.title.containsIgnoreCase(title);
    }

    private BooleanExpression containsContents(String contents){
        return (contents == null || contents.isBlank()) ? null : boastCatPost.contents.containsIgnoreCase(contents);
    }
}
