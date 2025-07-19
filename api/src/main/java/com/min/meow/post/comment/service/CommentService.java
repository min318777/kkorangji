package com.min.meow.post.comment.service;

import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.domain.response.RegisterCommentDto;
import com.min.meow.post.comment.domain.response.UpdateCommentDto;

public interface CommentService {
    public RegisterCommentDto registerLostCatPostComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId);
    public UpdateCommentDto updateLostCatPostComment(UpdateCommentRequest updateCommentRequest, Long lostCatPostCommentId);
    public void deleteLostCatPostComment(Long lostCatPostCommentId);
}
