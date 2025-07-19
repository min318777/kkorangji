package com.min.meow.post.boastcatpost.service;


import com.min.meow.post.boastcatpost.domain.response.BoastCatPostResponse;
import com.min.meow.post.boastcatpost.domain.response.CreateBoastCatPostResponse;
import com.min.meow.post.boastcatpost.domain.response.UpdateBoastCatPostResponse;
import com.min.meow.post.boastcatpost.domain.request.CreateBoastCatPostRequest;
import com.min.meow.post.boastcatpost.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.post.boastcatpost.entity.BoastCatPost;
import com.min.meow.post.boastcatpost.repository.BoastCatPostRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BoastCatPostService {
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;

    // 모든 글 조회
    public Page<BoastCatPostResponse> getAllBoastCatPosts(Pageable pageable){

        return boastCatPostRepository.findAll(pageable).map(BoastCatPostResponse::convertToDto);
    }

    // 글 작성
    @Transactional
    public CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, String loginId){

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        BoastCatPost boastCatPost = BoastCatPost.convertToEntity(createBoastCatPostRequest, writer);

        boastCatPostRepository.save(boastCatPost);

        return CreateBoastCatPostResponse.convertToDto(boastCatPost);
    }

    // 글 수정
    @Transactional
    public UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest, Long boastCatPostId, String loginId){

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        if(!boastCatPost.getUser().getLoginId().equals(loginId)){
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        boastCatPost.update(updateBoastCatPostRequest);

        return UpdateBoastCatPostResponse.convertToDto(boastCatPost);
    }

    // 글 삭제
    @Transactional
    public void deleteBoastCatPost(Long boastCatPostId, String loginId, String password){

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        if(!boastCatPost.getUser().getLoginId().equals(loginId)){
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        boastCatPostRepository.deleteById(boastCatPostId);
        /*
        if(!boastCatPostRepository.existsById(boastCatPostId)){
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
        boastCatPostRepository.deleteById(boastCatPostId);
         */
    }
}
