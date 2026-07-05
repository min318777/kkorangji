package com.min.meow.common;


public enum PostType {
    ALL("전체"),
    BOAST("고양이 자랑글"),
    LOST("실종 고양이글");

    private final String description;

    PostType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
