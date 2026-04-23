package com.min.meow.post.postlike.service;

import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.notification.event.LikeEvent;
import com.min.meow.notification.event.NotificationEventPublisher;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.postlike.entity.PostLike;
import com.min.meow.postlike.repository.PostLikeRepository;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요 DB 비동기 기록 담당
 *
 * LikeCountService와 반드시 별도 빈으로 분리해야 합니다.
 * 같은 클래스 내 self-invocation 시 @Async 프록시가 동작하지 않기 때문입니다.
 *
 * 흐름:
 * LikeCountService (Redis 즉각 응답) → LikeAsyncWriter (DB 비동기 기록)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeAsyncWriter {

    private final PostLikeRepository postLikeRepository;
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    /**
     * 좋아요 DB 기록 (비동기)
     *
     * liked=true  → PostLike INSERT + likeCount +1 + 알림 발행
     * liked=false → PostLike DELETE + likeCount -1
     *
     * @param postId  게시글 ID
     * @param userId  좋아요 누른 사용자 ID
     * @param liked   true: 좋아요 등록, false: 좋아요 취소
     */
    @Async("likeExecutor")
    @Transactional
    public void persist(Long postId, Long userId, boolean liked) {
        try {
            if (liked) {
                persistLike(postId, userId);
            } else {
                cancelLike(postId, userId);
            }
        } catch (DataIntegrityViolationException e) {
            // UniqueConstraint 위반 = Redis-DB 불일치 상황 (무시)
            log.warn("좋아요 중복 감지 (무시) - postId: {}, userId: {}", postId, userId);
        } catch (Exception e) {
            // 모든 예외 로깅 → 정합성 점검 시 추적 가능
            log.error("좋아요 DB 기록 실패 - postId: {}, userId: {}, liked: {}", postId, userId, liked, e);
        }
    }

    private void persistLike(Long postId, Long userId) {
        // 게시글+작성자 정보 한 번에 조회 (알림 발행에 필요)
        BoastCatPost post = boastCatPostRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        PostLike like = PostLike.builder()
                .user(userRepository.getReferenceById(userId))
                .boastCatPost(post)
                .build();
        postLikeRepository.save(like);
        boastCatPostRepository.incrementLikeCountByDelta(postId, 1);

        // 게시글 작성자가 탈퇴하지 않고, 본인 게시글이 아닌 경우에만 알림 발행
        if (!post.getUser().isWithdrawn() && !userId.equals(post.getUser().getId())) {
            LikeEvent event = new LikeEvent(like.getId(), postId, post.getUser().getId());
            notificationEventPublisher.publishLikeEvent(event);
            log.debug("좋아요 알림 발행 - postId: {}, receiverUserId: {}", postId, post.getUser().getId());
        }
    }

    private void cancelLike(Long postId, Long userId) {
        postLikeRepository.deleteByBoastCatPostIdAndUserId(postId, userId);
        boastCatPostRepository.incrementLikeCountByDelta(postId, -1);
    }
}
