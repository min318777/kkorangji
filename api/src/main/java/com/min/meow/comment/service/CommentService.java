package com.min.meow.comment.service;

import com.min.meow.comment.dto.request.RegisterCommentRequest;
import com.min.meow.comment.dto.request.UpdateCommentRequest;
import com.min.meow.comment.dto.response.GetCommentResponse;
import com.min.meow.comment.dto.response.RegisterCommentResponse;
import com.min.meow.comment.dto.response.UpdateCommentResponse;
import com.min.meow.global.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {
    // 고양이 자랑 게시글 댓글 관련 메서드
    List<GetCommentResponse> getBoastCatPostComment(Long boastCatPostId);
    PageResponse<GetCommentResponse> getBoastCatPostComment(Long boastCatPostId, Pageable pageable);
    RegisterCommentResponse registerBoastCatPostComment(RegisterCommentRequest registerCommentRequest, Long boastCatPostId, Long userId);

    // 실종 고양이 게시글 댓글 관련 메서드
    List<GetCommentResponse> getLostCatPostComment(Long lostCatPostId);
    PageResponse<GetCommentResponse> getLostCatPostComment(Long lostCatPostId, Pageable pageable);
    RegisterCommentResponse registerLostCatPostComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId, Long userId);

    // 공통 댓글 관리 메서드 (게시글 타입 무관)
    UpdateCommentResponse updateComment(UpdateCommentRequest updateCommentRequest, Long commentId, Long userId);
    void deleteComment(Long commentId, Long userId);
}
