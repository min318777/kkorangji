package com.min.meow.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 309975896L;

    public static final QUser user = new QUser("user");

    public final DateTimePath<java.time.LocalDateTime> anonymizedAt = createDateTime("anonymizedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isDelete = createBoolean("isDelete");

    public final DateTimePath<java.time.LocalDateTime> lastLoginAt = createDateTime("lastLoginAt", java.time.LocalDateTime.class);

    public final StringPath loginId = createString("loginId");

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final ListPath<com.min.meow.postlike.entity.PostLike, com.min.meow.postlike.entity.QPostLike> postLike = this.<com.min.meow.postlike.entity.PostLike, com.min.meow.postlike.entity.QPostLike>createList("postLike", com.min.meow.postlike.entity.PostLike.class, com.min.meow.postlike.entity.QPostLike.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> registeredAt = createDateTime("registeredAt", java.time.LocalDateTime.class);

    public final ListPath<UserRole, QUserRole> userRoles = this.<UserRole, QUserRole>createList("userRoles", UserRole.class, QUserRole.class, PathInits.DIRECT2);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

