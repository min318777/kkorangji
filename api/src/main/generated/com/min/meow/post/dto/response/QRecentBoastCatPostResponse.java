package com.min.meow.post.dto.response;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.min.meow.post.dto.response.QRecentBoastCatPostResponse is a Querydsl Projection type for RecentBoastCatPostResponse
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QRecentBoastCatPostResponse extends ConstructorExpression<RecentBoastCatPostResponse> {

    private static final long serialVersionUID = -1175197294L;

    public QRecentBoastCatPostResponse(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> title, com.querydsl.core.types.Expression<String> writer, com.querydsl.core.types.Expression<java.time.LocalDateTime> createdAt, com.querydsl.core.types.Expression<Integer> commentCount, com.querydsl.core.types.Expression<Integer> likeCount, com.querydsl.core.types.Expression<Integer> view) {
        super(RecentBoastCatPostResponse.class, new Class<?>[]{long.class, String.class, String.class, java.time.LocalDateTime.class, int.class, int.class, int.class}, id, title, writer, createdAt, commentCount, likeCount, view);
    }

}

