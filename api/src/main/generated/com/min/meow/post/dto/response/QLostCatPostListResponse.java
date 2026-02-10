package com.min.meow.post.dto.response;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.min.meow.post.dto.response.QLostCatPostListResponse is a Querydsl Projection type for LostCatPostListResponse
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QLostCatPostListResponse extends ConstructorExpression<LostCatPostListResponse> {

    private static final long serialVersionUID = -39319652L;

    public QLostCatPostListResponse(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> title, com.querydsl.core.types.Expression<String> writer, com.querydsl.core.types.Expression<String> catName, com.querydsl.core.types.Expression<String> lostLocation, com.querydsl.core.types.Expression<Integer> commentCount, com.querydsl.core.types.Expression<Integer> view, com.querydsl.core.types.Expression<Boolean> isCompleted, com.querydsl.core.types.Expression<java.time.LocalDateTime> createdAt) {
        super(LostCatPostListResponse.class, new Class<?>[]{long.class, String.class, String.class, String.class, String.class, int.class, int.class, boolean.class, java.time.LocalDateTime.class}, id, title, writer, catName, lostLocation, commentCount, view, isCompleted, createdAt);
    }

}

