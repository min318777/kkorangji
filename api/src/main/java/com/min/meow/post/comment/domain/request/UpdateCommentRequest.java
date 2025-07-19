package com.min.meow.post.comment.domain.request;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateCommentRequest {

    private String content;
}
