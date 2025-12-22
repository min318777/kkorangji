package com.min.meow.post.postlike.service;


import com.min.meow.kafka.producer.NotificationEventPublisher;
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
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public boolean toggleLike(Long boastCatPostId, String loginId){
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        Optional<PostLike> postLike = postLikeRepository.findByBoastCatPostIdAndLoginId(boastCatPostId, loginId);
        if(postLike.isPresent()){
            postLikeRepository.delete(postLike.get());
            return false;
        }
        PostLike like = PostLike.builder()
                .user(user)
                .boastCatPost(boastCatPost)
                .build();
        postLikeRepository.save(like);

        LikeEvent event = new LikeEvent(
                like.getId(),
                boastCatPostId,
                boastCatPost.getUser().getLoginId()
        );
        notificationEventPublisher.publishLikeEvent(event);
        return true;
    }
}
