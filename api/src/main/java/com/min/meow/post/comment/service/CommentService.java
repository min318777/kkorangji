package com.min.meow.post.comment.service;

import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.domain.response.GetCommentResponse;
import com.min.meow.post.comment.domain.response.RegisterCommentResponse;
import com.min.meow.post.comment.domain.response.UpdateCommentResponse;
import com.min.meow.post.comment.entity.Comment;

import java.util.List;

public interface CommentService {
    public List<GetCommentResponse> getBoastCatPostComment(Long boastCatPostId);
    public RegisterCommentResponse registerBoastCatPostComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId, String loginId);
    public UpdateCommentResponse updateLostCatPostComment(UpdateCommentRequest updateCommentRequest, Long lostCatPostCommentId);
    public void deleteLostCatPostComment(Long lostCatPostCommentId);
}
