package com.min.meow.notification;

public record CreateNotificationCommand (
        Long sourceId,
        Long postId,
        String receiverLoginId
){}

