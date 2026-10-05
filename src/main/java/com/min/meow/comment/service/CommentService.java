package com.min.meow.comment.service;

import com.min.meow.notification.event.NotificationEventPublisher;
import com.min.meow.notification.event.PopularScoreEvent;
import com.min.meow.common.NotificationType;
import com.min.meow.common.SecurityUtil;
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
import com.min.meow.common.PostType;
import com.min.meow.notification.dto.response.NotificationResponse;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.repository.NotificationRepository;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.post.service.PopularRankingService;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.common.PageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final NotificationRepository notificationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PopularRankingService popularRankingService;

    // 댓글 조회 (자랑글·실종글 공통, 원댓글 + 대댓글 2뎁스, 쿼리 2번으로 N+1 방지)
    public PageResponse<GetCommentResponse> getComments(Long postId, PostType postType, Pageable pageable) {
        Page<Comment> rootComments = commentRepository.findRootByPostIdAndPostType(postId, postType, pageable);
        return buildCommentResponse(rootComments, pageable);
    }

    // 원댓글 ID 목록으로 대댓글 일괄 조회 후 응답 조립
    private PageResponse<GetCommentResponse> buildCommentResponse(Page<Comment> rootComments, Pageable pageable) {
        List<Long> commentIds = rootComments.getContent().stream()
                .map(Comment::getId).toList();

        Map<Long, List<Comment>> repliesMap = commentIds.isEmpty()
                ? Map.of()
                : commentRepository.findRepliesByParentIds(commentIds).stream()
                        .collect(Collectors.groupingBy(r -> r.getParentComment().getId()));

        List<GetCommentResponse> responses = rootComments.getContent().stream()
                .map(c -> GetCommentResponse.toResponse(c, repliesMap.getOrDefault(c.getId(), List.of())))
                .toList();

        return PageResponse.from(new PageImpl<>(responses, pageable, rootComments.getTotalElements()));
    }

    // 댓글 작성
    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public RegisterCommentResponse registerComment(RegisterCommentRequest request, Long postId, PostType postType, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Comment parentComment = resolveParentComment(request.getParentCommentId(), postId, postType);

        Comment comment = Comment.builder()
                .contents(request.getContent())
                .user(user)
                .postId(postId)
                .postType(postType)
                .parentComment(parentComment)
                .build();
        commentRepository.save(comment);

        // postType에 따라 댓글 수 증가 및 알림 발행
        if (postType == PostType.BOAST) {
            BoastCatPost post = boastCatPostRepository.findById(postId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", postId)));
            boastCatPostRepository.incrementCommentCount(postId);

            if (!post.getUser().isWithdrawn() && !user.getId().equals(post.getUser().getId())) {
                notificationEventPublisher.publishCommentEvent(
                        new CommentEvent(comment.getId(), postId, PostType.BOAST, user.getNickname(), post.getUser().getId()));
            }
            // 인기글 Sorted Set 점수 +2
            notificationEventPublisher.publishPopularScoreEvent(new PopularScoreEvent(postId, 2));
        } else {
            LostCatPost post = lostCatRepository.findById(postId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", postId)));
            lostCatRepository.incrementCommentCount(postId);

            if (!post.getUser().isWithdrawn() && !user.getId().equals(post.getUser().getId())) {
                notificationEventPublisher.publishCommentEvent(
                        new CommentEvent(comment.getId(), postId, PostType.LOST, user.getNickname(), post.getUser().getId()));
            }
        }

        return RegisterCommentResponse.toResponse(comment);
    }

    // ========== 댓글 작성 + 알림 처리 통합 비교 (v1: 비교군 / v2: registerComment, 채택) ==========

    // v1: 댓글 저장 + 알림 저장 + SSE 발행을 하나의 트랜잭션 안에서 동기로 처리 (비교군)
    // registerComment(v2)와 달리 이벤트 발행(AFTER_COMMIT + @Async) 없이 즉시 처리하므로
    // 알림 저장 쿼리와 Redis Pub/Sub 호출이 끝날 때까지 DB 커넥션을 붙잡고 있는다.
    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public RegisterCommentResponse registerCommentV1(RegisterCommentRequest request, Long postId, PostType postType, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Comment parentComment = resolveParentComment(request.getParentCommentId(), postId, postType);

        Comment comment = Comment.builder()
                .contents(request.getContent())
                .user(user)
                .postId(postId)
                .postType(postType)
                .parentComment(parentComment)
                .build();
        commentRepository.save(comment);

        if (postType == PostType.BOAST) {
            BoastCatPost post = boastCatPostRepository.findById(postId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", postId)));
            boastCatPostRepository.incrementCommentCount(postId);

            if (!post.getUser().isWithdrawn() && !user.getId().equals(post.getUser().getId())) {
                saveAndSendNotificationSync(comment.getId(), postId, PostType.BOAST, user.getNickname(), post.getUser().getId());
            }
            // 인기글 Sorted Set 점수 +2 (v2는 이벤트로 비동기 반영, v1은 동기로 직접 반영)
            popularRankingService.incrementScoreSync(postId, 2);
        } else {
            LostCatPost post = lostCatRepository.findById(postId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", postId)));
            lostCatRepository.incrementCommentCount(postId);

            if (!post.getUser().isWithdrawn() && !user.getId().equals(post.getUser().getId())) {
                saveAndSendNotificationSync(comment.getId(), postId, PostType.LOST, user.getNickname(), post.getUser().getId());
            }
        }

        return RegisterCommentResponse.toResponse(comment);
    }

    // v1 비교군 전용: NotificationEventListener가 AFTER_COMMIT + @Async로 하던 일을
    // 같은 트랜잭션 안에서 동기로 수행 (알림 DB 저장 + Redis Pub/Sub 발행)
    private void saveAndSendNotificationSync(Long commentId, Long postId, PostType postType, String writer, Long receiverUserId) {
        Notification notification = Notification.builder()
                .sourceId(commentId)
                .postId(postId)
                .postType(postType)
                .receiverUserId(receiverUserId)
                .type(NotificationType.COMMENT)
                .message(String.format("%s님이 댓글을 남겼습니다.", writer))
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        try {
            String channel = "sse:notify:" + saved.getReceiverUserId();
            String payload = objectMapper.writeValueAsString(NotificationResponse.from(saved));
            redisTemplate.convertAndSend(channel, payload);
        } catch (JsonProcessingException e) {
            log.error("[v1 비교군] 알림 직렬화 실패 - receiverUserId: {}", receiverUserId, e);
        }
    }

    // 댓글 수정 (삭제된 댓글은 수정 불가)
    @Transactional
    public UpdateCommentResponse updateComment(UpdateCommentRequest updateCommentRequest, Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COMMENT, Map.of("commentId", commentId)));

        if (comment.isDeleted()) {
            throw new CustomException(ErrorCode.NOT_FOUND_COMMENT, Map.of("commentId", commentId, "deleted", true));
        }
        if (!comment.isAuthor(userId) && !SecurityUtil.hasAuthority("comment:update")) {
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
    public void deleteComment(Long commentId, Long userId, boolean hasDeleteAuthority) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COMMENT, Map.of("commentId", commentId)));

        if (!comment.isAuthor(userId) && !hasDeleteAuthority) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        if (comment.getParentComment() != null) {
            deleteReply(comment);
        } else {
            deleteRootComment(comment);
        }
    }

    // 대댓글 즉시 삭제 후 부모 연쇄 삭제 여부 확인
    private void deleteReply(Comment reply) {
        Comment parent = reply.getParentComment();
        decrementPostCommentCount(reply);
        commentRepository.delete(reply);

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

    // 게시글 댓글 수 감소 + 인기글 점수 차감 (자랑글만)
    private void decrementPostCommentCount(Comment comment) {
        if (comment.getPostType() == PostType.BOAST) {
            boastCatPostRepository.decrementCommentCount(comment.getPostId());
            notificationEventPublisher.publishPopularScoreEvent(
                    new PopularScoreEvent(comment.getPostId(), -2));
        } else {
            lostCatRepository.decrementCommentCount(comment.getPostId());
        }
    }

    // 부모 댓글 유효성 검증 (2뎁스 제한, 같은 게시글 여부 확인)
    private Comment resolveParentComment(Long parentCommentId, Long postId, PostType postType) {
        if (parentCommentId == null) return null;

        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COMMENT, Map.of("parentCommentId", parentCommentId)));

        if (parent.getParentComment() != null) {
            throw new CustomException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
        }
        if (parent.isDeleted()) {
            throw new CustomException(ErrorCode.NOT_FOUND_COMMENT, Map.of("parentCommentId", parentCommentId, "deleted", true));
        }
        // 같은 게시글의 댓글인지 확인
        if (!parent.getPostId().equals(postId) || parent.getPostType() != postType) {
            throw new CustomException(ErrorCode.NOT_FOUND_COMMENT,
                    Map.of("parentCommentId", parentCommentId, "postId", postId, "postType", postType));
        }
        return parent;
    }
}
