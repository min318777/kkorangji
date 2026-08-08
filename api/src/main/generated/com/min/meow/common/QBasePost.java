package com.min.meow.common;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBasePost is a Querydsl query type for BasePost
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QBasePost extends EntityPathBase<BasePost> {

    private static final long serialVersionUID = -1553551025L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBasePost basePost = new QBasePost("basePost");

    public final NumberPath<Integer> commentCount = createNumber("commentCount", Integer.class);

    public final StringPath contents = createString("contents");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final com.min.meow.user.entity.QUser user;

    public final NumberPath<Integer> view = createNumber("view", Integer.class);

    public QBasePost(String variable) {
        this(BasePost.class, forVariable(variable), INITS);
    }

    public QBasePost(Path<? extends BasePost> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBasePost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBasePost(PathMetadata metadata, PathInits inits) {
        this(BasePost.class, metadata, inits);
    }

    public QBasePost(Class<? extends BasePost> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.min.meow.user.entity.QUser(forProperty("user")) : null;
    }

}

