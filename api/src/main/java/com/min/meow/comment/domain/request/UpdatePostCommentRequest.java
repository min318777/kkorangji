package com.min.meow.comment.domain.request;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdatePostCommentRequest {

    private String content;
}
