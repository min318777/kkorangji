package com.min.meow.post.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLostCatPost is a Querydsl query type for LostCatPost
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLostCatPost extends EntityPathBase<LostCatPost> {

    private static final long serialVersionUID = -1889269072L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLostCatPost lostCatPost = new QLostCatPost("lostCatPost");

    public final com.min.meow.global.QBasePost _super;

    public final NumberPath<Integer> catAge = createNumber("catAge", Integer.class);

    public final StringPath catColor = createString("catColor");

    public final StringPath catName = createString("catName");

    public final StringPath catType = createString("catType");

    public final NumberPath<Integer> catWeight = createNumber("catWeight", Integer.class);

    public final NumberPath<Integer> commentCount = createNumber("commentCount", Integer.class);

    public final ListPath<com.min.meow.comment.entity.Comment, com.min.meow.comment.entity.QComment> comments = this.<com.min.meow.comment.entity.Comment, com.min.meow.comment.entity.QComment>createList("comments", com.min.meow.comment.entity.Comment.class, com.min.meow.comment.entity.QComment.class, PathInits.DIRECT2);

    //inherited
    public final StringPath contents;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt;

    //inherited
    public final NumberPath<Long> id;

    public final ListPath<String, StringPath> imageUrls = this.<String, StringPath>createList("imageUrls", String.class, StringPath.class, PathInits.DIRECT2);

    public final BooleanPath isCompleted = createBoolean("isCompleted");

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final StringPath lostLocation = createString("lostLocation");

    public final NumberPath<Integer> reward = createNumber("reward", Integer.class);

    //inherited
    public final StringPath title;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt;

    // inherited
    public final com.min.meow.user.entity.QUser user;

    //inherited
    public final NumberPath<Integer> view;

    public QLostCatPost(String variable) {
        this(LostCatPost.class, forVariable(variable), INITS);
    }

    public QLostCatPost(Path<? extends LostCatPost> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLostCatPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLostCatPost(PathMetadata metadata, PathInits inits) {
        this(LostCatPost.class, metadata, inits);
    }

    public QLostCatPost(Class<? extends LostCatPost> type, PathMetadata metadata, PathInits inits) {
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

