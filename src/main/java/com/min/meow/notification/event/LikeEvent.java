package com.min.meow.notification.event;

import com.min.meow.common.NotificationType;
import com.min.meow.common.PostType;

public record LikeEvent (
    Long likeId,
    Long postId,
    PostType postType,
    Long receiverUserId
) implements NotificationEvent {

    @Override
    public Long sourceId() {
        return likeId;
    }

    @Override
    public NotificationType type() {
        return NotificationType.LIKE;
    }

    @Override
    public String message() {
        return "게시글에 좋아요가 추가되었습니다.";
    }
}
