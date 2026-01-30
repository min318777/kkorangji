package com.min.meow.notification.event;

public record CommentEvent(
    Long commentId,
    Long postId,
    String writer,
    String receiverLoginId
){}
