package com.min.meow.post.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoastCatPost is a Querydsl query type for BoastCatPost
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoastCatPost extends EntityPathBase<BoastCatPost> {

    private static final long serialVersionUID = -547476573L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoastCatPost boastCatPost = new QBoastCatPost("boastCatPost");

    public final com.min.meow.global.QBasePost _super;

    public final ListPath<com.min.meow.comment.entity.Comment, com.min.meow.comment.entity.QComment> comments = this.<com.min.meow.comment.entity.Comment, com.min.meow.comment.entity.QComment>createList("comments", com.min.meow.comment.entity.Comment.class, com.min.meow.comment.entity.QComment.class, PathInits.DIRECT2);

    //inherited
    public final StringPath contents;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt;

    //inherited
    public final NumberPath<Long> id;

    public final ListPath<String, StringPath> imageUrls = this.<String, StringPath>createList("imageUrls", String.class, StringPath.class, PathInits.DIRECT2);

    public final ListPath<com.min.meow.postlike.entity.PostLike, com.min.meow.postlike.entity.QPostLike> postLikeList = this.<com.min.meow.postlike.entity.PostLike, com.min.meow.postlike.entity.QPostLike>createList("postLikeList", com.min.meow.postlike.entity.PostLike.class, com.min.meow.postlike.entity.QPostLike.class, PathInits.DIRECT2);

    //inherited
    public final StringPath title;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt;

    // inherited
    public final com.min.meow.user.entity.QUser user;

    //inherited
    public final NumberPath<Integer> view;

    public QBoastCatPost(String variable) {
        this(BoastCatPost.class, forVariable(variable), INITS);
    }

    public QBoastCatPost(Path<? extends BoastCatPost> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoastCatPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoastCatPost(PathMetadata metadata, PathInits inits) {
        this(BoastCatPost.class, metadata, inits);
    }

    public QBoastCatPost(Class<? extends BoastCatPost> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new com.min.meow.global.QBasePost(type, metadata, inits);
        this.contents = _super.contents;
        this.createdAt = _super.createdAt;
        this.id = _super.id;
        this.title = _super.title;
        this.updatedAt = _super.updatedAt;
        this.user = _super.user;
        this.view = _super.view;
    }

}

