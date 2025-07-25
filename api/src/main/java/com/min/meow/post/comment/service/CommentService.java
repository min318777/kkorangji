package com.min.meow.post.comment.service;

import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.domain.response.RegisterCommentResponse;
import com.min.meow.post.comment.domain.response.UpdateCommentResponse;

public interface CommentService {
    public RegisterCommentResponse registerLostCatPostComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId);
    public UpdateCommentResponse updateLostCatPostComment(UpdateCommentRequest updateCommentRequest, Long lostCatPostCommentId);
    public void deleteLostCatPostComment(Long lostCatPostCommentId);
}
