package com.min.kafka.topic;

public enum KafkaTopic {
    COMMENT_NOTIFICATION("comment-notification"),
    LIKE_NOTIFICATION("like-notification");

    private final String topic;

    KafkaTopic(String topic){
        this.topic = topic;
    }
    public String getTopic() {
        return topic;
    }

}
