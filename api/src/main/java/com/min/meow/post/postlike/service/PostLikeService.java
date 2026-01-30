package com.min.meow.post.postlike.service;


import com.min.meow.notification.event.NotificationEventPublisher;
import com.min.meow.notification.event.LikeEvent;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.postlike.entity.PostLike;
import com.min.meow.postlike.repository.PostLikeRepository;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;

    public Integer getLikeCount(Long boastCatPostId){
        BoastCatPost post = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        return postLikeRepository.countByBoastCatPost(post);
    }

    @Transactional
    public boolean toggleLike(Long boastCatPostId, String loginId){
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        Optional<PostLike> postLike = postLikeRepository.findByBoastCatPostIdAndLoginId(boastCatPostId, loginId);
        if(postLike.isPresent()){
            postLikeRepository.delete(postLike.get());
            boastCatPost.decrementLikeCount();  // 좋아요 수 감소
            return false;
        }
        PostLike like = PostLike.builder()
                .user(user)
                .boastCatPost(boastCatPost)
                .build();
        postLikeRepository.save(like);
        boastCatPost.incrementLikeCount();  // 좋아요 수 증가

        // 게시글 작성자가 탈퇴하지 않은 경우에만 알림 발송
        // - 탈퇴한 사용자에게는 알림을 보내지 않음
        // - 자기 자신의 게시글에 좋아요를 누를 경우도 알림 발송하지 않음
        if (!boastCatPost.getUser().isWithdrawn() && !loginId.equals(boastCatPost.getUser().getLoginId())) {
            LikeEvent event = new LikeEvent(
                    like.getId(),
                    boastCatPostId,
                    boastCatPost.getUser().getLoginId()
            );
            notificationEventPublisher.publishLikeEvent(event);
            log.debug("좋아요 알림 발송 - postId: {}, receiver: {}", boastCatPostId, boastCatPost.getUser().getLoginId());
        } else {
            log.debug("좋아요 알림 미발송 - 게시글 작성자 탈퇴 또는 본인 좋아요");
        }
        return true;
    }
}
