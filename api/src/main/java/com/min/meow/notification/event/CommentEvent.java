package com.min.meow.notification.event;

import com.min.meow.common.PostType;

public record CommentEvent(
    Long commentId,
    Long postId,
    PostType postType,
    String writer,
    Long receiverUserId
){}
