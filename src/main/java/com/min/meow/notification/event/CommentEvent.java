package com.min.meow.notification.event;

import com.min.meow.common.NotificationType;
import com.min.meow.common.PostType;

public record CommentEvent(
    Long commentId,
    Long postId,
    PostType postType,
    String writer,
    Long receiverUserId
) implements NotificationEvent {

    @Override
    public Long sourceId() {
        return commentId;
    }

    @Override
    public NotificationType type() {
        return NotificationType.COMMENT;
    }

    @Override
    public String message() {
        return String.format("%s님이 댓글을 남겼습니다.", writer);
    }
}
