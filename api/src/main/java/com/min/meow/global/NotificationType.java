package com.min.meow.global;

public enum NotificationType {
    COMMENT("댓글"),
    LIKE("좋아요");

    private final String description;

    NotificationType(String description){
        this.description = description;
    }
}
