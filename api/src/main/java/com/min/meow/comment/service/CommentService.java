package com.min.meow.comment.service;

import com.min.meow.comment.dto.request.RegisterCommentRequest;
import com.min.meow.comment.dto.request.UpdateCommentRequest;
import com.min.meow.comment.dto.response.GetCommentResponse;
import com.min.meow.comment.dto.response.RegisterCommentResponse;
import com.min.meow.comment.dto.response.UpdateCommentResponse;

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
