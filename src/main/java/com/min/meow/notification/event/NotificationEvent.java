package com.min.meow.notification.event;

import com.min.meow.common.NotificationType;
import com.min.meow.common.PostType;

public interface NotificationEvent {
    Long sourceId();
    Long postId();
    PostType postType();
    Long receiverUserId();
    NotificationType type();
    String message();
}
