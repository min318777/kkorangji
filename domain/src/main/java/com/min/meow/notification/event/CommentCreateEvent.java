package com.min.meow.notification.event;

public record CommentCreateEvent (
    Long commentId,
    Long postId,
    String receiverLoginId
){}
