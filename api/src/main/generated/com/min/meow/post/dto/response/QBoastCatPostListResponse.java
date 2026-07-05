package com.min.meow.post.dto.response;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.min.meow.post.dto.response.QBoastCatPostListResponse is a Querydsl Projection type for BoastCatPostListResponse
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QBoastCatPostListResponse extends ConstructorExpression<BoastCatPostListResponse> {

    private static final long serialVersionUID = 2088567317L;

    public QBoastCatPostListResponse(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> title, com.querydsl.core.types.Expression<Integer> likeCount, com.querydsl.core.types.Expression<Integer> commentCount, com.querydsl.core.types.Expression<Integer> view, com.querydsl.core.types.Expression<java.time.LocalDateTime> createdAt, com.querydsl.core.types.Expression<String> thumbnailUrl) {
        super(BoastCatPostListResponse.class, new Class<?>[]{long.class, String.class, int.class, int.class, int.class, java.time.LocalDateTime.class, String.class}, id, title, likeCount, commentCount, view, createdAt, thumbnailUrl);
    }

}

