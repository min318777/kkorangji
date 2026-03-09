package com.min.meow.comment.service.impl;

import com.min.meow.notification.event.NotificationEventPublisher;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.comment.dto.request.RegisterCommentRequest;
import com.min.meow.comment.dto.request.UpdateCommentRequest;
import com.min.meow.comment.dto.response.GetCommentResponse;
import com.min.meow.comment.dto.response.RegisterCommentResponse;
import com.min.meow.comment.dto.response.UpdateCommentResponse;
import com.min.meow.notification.event.CommentEvent;
import com.min.meow.comment.entity.Comment;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.comment.service.CommentService;
import com.min.meow.global.SecurityUtil;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.global.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final LostCatRepository lostCatRepository;
    private final BoastCatPostRepository boastCatPostRepository;
    private final CommentRepository commentRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserRepository userRepository;

    // 고양이 자랑 게시글 댓글 조회 (전체)
    @Override
    public List<GetCommentResponse> getBoastCatPostComment(Long boastCatPostId) {
        List<Comment> comments = commentRepository.findByBoastCatPostIdWithUser(boastCatPostId);
        return comments.stream()
                .map(GetCommentResponse::toResponse)
                .collect(Collectors.toList());
    }

    // 고양이 자랑 게시글 댓글 조회 (페이지네이션)
    @Override
    public PageResponse<GetCommentResponse> getBoastCatPostComment(Long boastCatPostId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByBoastCatPostIdWithUser(boastCatPostId, pageable);
        return PageResponse.from(comments.map(GetCommentResponse::toResponse));
    }

    // 실종 고양이 게시글 댓글 조회 (전체)
    @Override
    public List<GetCommentResponse> getLostCatPostComment(Long lostCatPostId) {
        List<Comment> comments = commentRepository.findByLostCatPostIdWithUser(lostCatPostId);
        return comments.stream()
                .map(GetCommentResponse::toResponse)
                .collect(Collectors.toList());
    }

    // 실종 고양이 게시글 댓글 조회 (페이지네이션)
    @Override
    public PageResponse<GetCommentResponse> getLostCatPostComment(Long lostCatPostId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByLostCatPostIdWithUser(lostCatPostId, pageable);
        return PageResponse.from(comments.map(GetCommentResponse::toResponse));
    }

    // 고양이 자랑 게시글 댓글 작성
    @Transactional
    @Override
    public RegisterCommentResponse registerBoastCatPostComment(RegisterCommentRequest registerCommentRequest, Long boastCatPostId, Long userId){
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 엔티티 직접 생성 (toEntity 메서드 제거됨)
        Comment comment = Comment.builder()
                .contents(registerCommentRequest.getContent())
                .user(user)
                .isRead(false)
                .boastCatPost(boastCatPost)
                .build();
        boastCatPost.getComments().add(comment);
        boastCatPost.incrementCommentCount();  // 댓글 수 증가
        commentRepository.save(comment);

        // 게시글 작성자가 탈퇴하지 않은 경우에만 알림 발송
        // - 탈퇴한 사용자에게는 알림을 보내지 않음
        // - 자기 자신의 게시글에 댓글을 달 경우도 알림 발송하지 않음
        if (!boastCatPost.getUser().isWithdrawn() && !user.getLoginId().equals(boastCatPost.getUser().getLoginId())) {
            CommentEvent event = new CommentEvent(
                    comment.getId(),
                    boastCatPostId,
                    user.getLoginId(),
                    boastCatPost.getUser().getLoginId()
            );
            notificationEventPublisher.publishCommentEvent(event);
            log.debug("댓글 알림 발송 - postId: {}, receiver: {}", boastCatPostId, boastCatPost.getUser().getLoginId());
        } else {
            log.debug("댓글 알림 미발송 - 게시글 작성자 탈퇴 또는 본인 댓글");
        }
        return RegisterCommentResponse.toResponse(comment);
    }

    // 실종 고양이 게시글 댓글 작성
    @Transactional
    @Override
    public RegisterCommentResponse registerLostCatPostComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId, Long userId){
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));


        // 엔티티 직접 생성 (toEntity 메서드 제거됨)
        Comment comment = Comment.builder()
                .contents(registerCommentRequest.getContent())
                .user(user)
                .isRead(false)
                .lostCatPost(lostCatPost)
                .build();
        lostCatPost.getComments().add(comment);
        lostCatPost.incrementCommentCount();  // 댓글 수 증가
        commentRepository.save(comment);

        // 게시글 작성자가 탈퇴하지 않은 경우에만 알림 발송
        // - 탈퇴한 사용자에게는 알림을 보내지 않음
        // - 자기 자신의 게시글에 댓글을 달 경우도 알림 발송하지 않음
        if (!lostCatPost.getUser().isWithdrawn() && !user.getLoginId().equals(lostCatPost.getUser().getLoginId())) {
            CommentEvent event = new CommentEvent(
                    comment.getId(),
                    lostCatPostId,
                    user.getLoginId(),
                    lostCatPost.getUser().getLoginId()
            );
            notificationEventPublisher.publishCommentEvent(event);
            log.debug("댓글 알림 발송 - postId: {}, receiver: {}", lostCatPostId, lostCatPost.getUser().getLoginId());
        } else {
            log.debug("댓글 알림 미발송 - 게시글 작성자 탈퇴 또는 본인 댓글");
        }
        return RegisterCommentResponse.toResponse(comment);
    }

    // 댓글 수정
    @Transactional
    @Override
    public UpdateCommentResponse updateComment(UpdateCommentRequest updateCommentRequest, Long commentId, Long userId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 수정은 본인만 허용 (관리자도 타인 댓글 수정 불가)
        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        comment.updateContent(updateCommentRequest.getContent());

        return UpdateCommentResponse.toResponse(comment);
    }

    // 댓글 삭제
    @Transactional
    @Override
    public void deleteComment(Long commentId, Long userId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 본인이 아니고 관리자 권한(comment:delete)도 없으면 → 403
        if (!comment.isAuthor(userId)
                && !SecurityUtil.hasAuthority("comment:delete")) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        // 게시글의 댓글 수 감소
        if (comment.getBoastCatPost() != null) {
            comment.getBoastCatPost().decrementCommentCount();
        }
        if (comment.getLostCatPost() != null) {
            comment.getLostCatPost().decrementCommentCount();
        }

        commentRepository.deleteById(commentId);
    }
}
