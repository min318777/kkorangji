package com.min.meow.global;

import com.min.meow.postcomment.entity.PostComment;

public interface BasePost {

    Long getId();

    void addComment(PostComment postComment);
    void removeComment(PostComment postComment);
}
