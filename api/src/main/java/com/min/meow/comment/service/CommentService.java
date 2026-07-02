package com.min.meow.comment.service;

import com.min.meow.notification.event.NotificationEventPublisher;
import com.min.meow.notification.event.PopularScoreEvent;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.comment.dto.request.RegisterCommentRequest;
import com.min.meow.comment.dto.request.UpdateCommentRequest;
import com.min.meow.comment.dto.response.GetCommentResponse;
import com.min.meow.comment.dto.response.RegisterCommentResponse;
import com.min.meow.comment.dto.response.UpdateCommentResponse;
import com.min.meow.notification.event.CommentEvent;
import com.min.meow.comment.entity.Comment;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.common.SecurityUtil;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.common.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final LostCatRepository lostCatRepository;
    private final BoastCatPostRepository boastCatPostRepository;
    private final CommentRepository commentRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserRepository userRepository;

    // 자랑글 댓글 조회 (원댓글 + 대댓글 2뎁스, 쿼리 2번으로 N+1 방지)
    public PageResponse<GetCommentResponse> getBoastCatPostComment(Long boastCatPostId, Pageable pageable) {
        Page<Comment> rootComments = commentRepository.findRootByBoastCatPostId(boastCatPostId, pageable);
        return buildCommentResponse(rootComments, pageable);
    }

    // 실종글 댓글 조회 
    public PageResponse<GetCommentResponse> getLostCatPostComment(Long lostCatPostId, Pageable pageable) {
        Page<Comment> rootComments = commentRepository.findRootByLostCatPostId(lostCatPostId, pageable);
        return buildCommentResponse(rootComments, pageable);
    }

    // 원댓글 ID 목록으로 대댓글 일괄 조회 후 응답
    private PageResponse<GetCommentResponse> buildCommentResponse(Page<Comment> rootComments, Pageable pageable) {
        List<Long> commentIds = rootComments.getContent().stream()
                .map(Comment::getId).toList();

        // 대댓글 일괄 조회 (쿼리 1번)
        Map<Long, List<Comment>> repliesMap = commentIds.isEmpty()
                ? Map.of()
                : commentRepository.findRepliesByParentIds(commentIds).stream()
                        .collect(Collectors.groupingBy(r -> r.getParentComment().getId()));

        List<GetCommentResponse> responses = rootComments.getContent().stream()
                .map(c -> GetCommentResponse.toResponse(c, repliesMap.getOrDefault(c.getId(), List.of())))
                .toList();

        return PageResponse.from(new PageImpl<>(responses, pageable, rootComments.getTotalElements()));
    }

    // 자랑글 댓글 작성 (원댓글 / 대댓글 공통 처리)
    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public RegisterCommentResponse registerBoastCatPostComment(RegisterCommentRequest request, Long boastCatPostId, Long userId) {
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Comment parentComment = resolveParentComment(request.getParentCommentId(), boastCatPostId, null);

        Comment comment = Comment.builder()
                .contents(request.getContent())
                .user(user)

                .boastCatPost(boastCatPost)
                .parentComment(parentComment)
                .build();
        commentRepository.save(comment);
        boastCatPostRepository.incrementCommentCount(boastCatPostId);

        if (!boastCatPost.getUser().isWithdrawn() && !user.getId().equals(boastCatPost.getUser().getId())) {
            notificationEventPublisher.publishCommentEvent(new CommentEvent(
                    comment.getId(), boastCatPostId, user.getLoginId(), boastCatPost.getUser().getId()));
        }

        // 인기글 Sorted Set 점수 +2 (AFTER_COMMIT 비동기 처리)
        notificationEventPublisher.publishPopularScoreEvent(new PopularScoreEvent(boastCatPostId, 2));

        return RegisterCommentResponse.toResponse(comment);
    }

    // 실종글 댓글 작성 (원댓글 / 대댓글 공통 처리)
    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public RegisterCommentResponse registerLostCatPostComment(RegisterCommentRequest request, Long lostCatPostId, Long userId) {
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Comment parentComment = resolveParentComment(request.getParentCommentId(), null, lostCatPostId);

        Comment comment = Comment.builder()
                .contents(request.getContent())
                .user(user)

                .lostCatPost(lostCatPost)
                .parentComment(parentComment)
                .build();
        commentRepository.save(comment);
        lostCatRepository.incrementCommentCount(lostCatPostId);

        if (!lostCatPost.getUser().isWithdrawn() && !user.getId().equals(lostCatPost.getUser().getId())) {
            notificationEventPublisher.publishCommentEvent(new CommentEvent(
                    comment.getId(), lostCatPostId, user.getLoginId(), lostCatPost.getUser().getId()));
        }
        return RegisterCommentResponse.toResponse(comment);
    }

    // 댓글 수정 (삭제된 댓글은 수정 불가)
    @Transactional
    public UpdateCommentResponse updateComment(UpdateCommentRequest updateCommentRequest, Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COMMENT));

        if (comment.isDeleted()) {
            throw new CustomException(ErrorCode.NOT_FOUND_COMMENT);
        }
        if (!comment.isAuthor(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        comment.updateContent(updateCommentRequest.getContent());
        return UpdateCommentResponse.toResponse(comment);
    }

    // 댓글 삭제
    // - 대댓글: 항상 즉시 삭제 → 부모가 소프트삭제 + 활성 대댓글 0개면 부모도 삭제
    // - 원댓글: 활성 대댓글 없으면 즉시 삭제, 있으면 소프트 삭제
    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COMMENT));

        if (!comment.isAuthor(userId) && !SecurityUtil.hasAuthority("comment:delete")) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        if (comment.getParentComment() != null) {
            // 대댓글: 즉시 삭제
            deleteReply(comment);
        } else {
            // 원댓글: 활성 대댓글 여부에 따라 분기
            deleteRootComment(comment);
        }
    }

    // 대댓글 즉시 삭제 후 부모 연쇄 삭제 여부 확인
    private void deleteReply(Comment reply) {
        Comment parent = reply.getParentComment();
        decrementPostCommentCount(reply);
        commentRepository.delete(reply);

        // 부모가 소프트삭제 상태이고 활성 대댓글이 더 없으면 부모도 삭제
        if (parent.isDeleted() && commentRepository.countActiveRepliesByParentId(parent.getId()) == 0) {
            decrementPostCommentCount(parent);
            commentRepository.delete(parent);
        }
    }

    // 원댓글 삭제: 활성 대댓글 없으면 즉시, 있으면 소프트 삭제
    private void deleteRootComment(Comment comment) {
        long activeReplies = commentRepository.countActiveRepliesByParentId(comment.getId());
        if (activeReplies == 0) {
            decrementPostCommentCount(comment);
            commentRepository.delete(comment);
        } else {
            comment.softDelete();
        }
    }

    // 게시글 댓글 수 감소 + 인기글 점수 -2 (자랑글만)
    private void decrementPostCommentCount(Comment comment) {
        if (comment.getBoastCatPost() != null) {
            boastCatPostRepository.decrementCommentCount(comment.getBoastCatPost().getId());
            // 인기글 Sorted Set 점수 -2 (AFTER_COMMIT 비동기 처리)
            notificationEventPublisher.publishPopularScoreEvent(
                    new PopularScoreEvent(comment.getBoastCatPost().getId(), -2));
        }
        if (comment.getLostCatPost() != null) {
            lostCatRepository.decrementCommentCount(comment.getLostCatPost().getId());
        }
    }

    // 부모 댓글 유효성 검증 (2뎁스 제한, 같은 게시글 여부 확인)
    private Comment resolveParentComment(Long parentCommentId, Long boastCatPostId, Long lostCatPostId) {
        if (parentCommentId == null) return null;

        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COMMENT));

        // 대댓글에 대댓글 금지 (2뎁스 제한)
        if (parent.getParentComment() != null) {
            throw new CustomException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
        }
        // 삭제된 댓글에 대댓글 금지
        if (parent.isDeleted()) {
            throw new CustomException(ErrorCode.NOT_FOUND_COMMENT);
        }
        // 같은 게시글의 댓글인지 확인
        if (boastCatPostId != null && (parent.getBoastCatPost() == null || !parent.getBoastCatPost().getId().equals(boastCatPostId))) {
            throw new CustomException(ErrorCode.NOT_FOUND_COMMENT);
        }
        if (lostCatPostId != null && (parent.getLostCatPost() == null || !parent.getLostCatPost().getId().equals(lostCatPostId))) {
            throw new CustomException(ErrorCode.NOT_FOUND_COMMENT);
        }
        return parent;
    }
}