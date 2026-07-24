package com.min.meow.postlike.service;

import com.min.meow.common.PostType;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.notification.event.LikeEvent;
import com.min.meow.notification.event.NotificationEventPublisher;
import com.min.meow.notification.event.PopularScoreEvent;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.postlike.entity.PostLike;
import com.min.meow.postlike.repository.PostLikeRepository;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public Long addLike(Long postId, Long userId) {
        if (postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)) {
            throw new CustomException(ErrorCode.ALREADY_LIKED);
        }

        BoastCatPost post = boastCatPostRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        try {
            PostLike like = PostLike.builder()
                    .user(userRepository.getReferenceById(userId))
                    .boastCatPost(post)
                    .build();
            postLikeRepository.save(like);
            boastCatPostRepository.updateLikeCount(postId, 1);

            // 본인 게시글이거나 탈퇴 회원이면 알림 생략
            if (!post.getUser().isWithdrawn() && !userId.equals(post.getUser().getId())) {
                notificationEventPublisher.publishLikeEvent(
                        new LikeEvent(like.getId(), postId, PostType.BOAST, post.getUser().getId()));
            }

            // 인기글 Sorted Set 점수 +3 (AFTER_COMMIT 비동기 처리)
            notificationEventPublisher.publishPopularScoreEvent(new PopularScoreEvent(postId, 3));

            // JPQL UPDATE는 1차 캐시를 우회하므로 계산으로 반환
            return post.getLikeCount() + 1L;

        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 인한 UniqueConstraint 위반 처리
            throw new CustomException(ErrorCode.ALREADY_LIKED);
        }
    }

    // 좋아요 취소
    @Transactional
    public Long cancelLike(Long postId, Long userId) {
        if (!postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)) {
            throw new CustomException(ErrorCode.NOT_LIKED);
        }

        BoastCatPost post = boastCatPostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        postLikeRepository.deleteByBoastCatPostIdAndUserId(postId, userId);
        boastCatPostRepository.updateLikeCount(postId, -1);

        // 인기글 Sorted Set 점수 -3 (AFTER_COMMIT 비동기 처리)
        notificationEventPublisher.publishPopularScoreEvent(new PopularScoreEvent(postId, -3));

        // JPQL UPDATE는 1차 캐시를 우회하므로 계산으로 반환, 최소 0 보장
        return Math.max(0L, post.getLikeCount() - 1);
    }

    // 좋아요 여부 확인
    @Transactional(readOnly = true)
    public boolean isLiked(Long postId, Long userId) {
        return postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId);
    }
}
