package com.min.meow.notification.event;

public record LikeEvent (
    Long likeId,
    Long postId,
    Long receiverUserId

){}
