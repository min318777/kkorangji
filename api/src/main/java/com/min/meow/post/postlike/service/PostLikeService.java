package com.min.meow.post.postlike.service;


import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.postlike.entity.PostLike;
import com.min.meow.post.postlike.repository.PostLikeRepository;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;

    public Integer getLikeCount(Long boastCatPostId){
        BoastCatPost post = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        return postLikeRepository.countByBoastCatPost(post);
    }

    public boolean toggleLike(Long boastCatPostId, User user){

        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        User persistUser = userRepository.findByLoginId(user.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        Optional<PostLike> postLike = postLikeRepository.findByBoastCatPostIdAndLoginId(boastCatPostId, user.getLoginId());
        if(postLike.isPresent()){
            postLikeRepository.delete(postLike.get());
            return false;
        }
        PostLike like = PostLike.builder()
                .user(persistUser)
                .boastCatPost(boastCatPost)
                .build();

        postLikeRepository.save(like);
        return true;
    }
}
