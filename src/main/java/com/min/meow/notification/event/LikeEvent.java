package com.min.meow.notification.event;

import com.min.meow.common.PostType;

public record LikeEvent (
    Long likeId,
    Long postId,
    PostType postType,
    Long receiverUserId
){}
