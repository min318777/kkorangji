package com.min.meow.postcomment.domain.request;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePostCommentRequest {

    private String content;
}
