package com.min.meow.post.comment.service;

import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.domain.response.GetCommentResponse;
import com.min.meow.post.comment.domain.response.RegisterCommentResponse;
import com.min.meow.post.comment.domain.response.UpdateCommentResponse;
import com.min.meow.post.comment.entity.Comment;

import java.util.List;

public interface CommentService {
    // 고양이 자랑 게시글 댓글 관련 메서드
    public List<GetCommentResponse> getBoastCatPostComment(Long boastCatPostId);
    public RegisterCommentResponse registerBoastCatPostComment(RegisterCommentRequest registerCommentRequest, Long boastCatPostId, String loginId);

    // 실종 고양이 게시글 댓글 관련 메서드
    public List<GetCommentResponse> getLostCatPostComment(Long lostCatPostId);
    public RegisterCommentResponse registerLostCatPostComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId, String loginId);

    // 공통 댓글 관리 메서드 (게시글 타입 무관)
    public UpdateCommentResponse updateComment(UpdateCommentRequest updateCommentRequest, Long commentId);
    public void deleteComment(Long commentId);
}
