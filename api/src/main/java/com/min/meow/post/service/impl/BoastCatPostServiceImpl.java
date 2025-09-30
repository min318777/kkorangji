package com.min.meow.post.service.impl;


import com.min.meow.config.S3Uploader;
import com.min.meow.global.PageResponse;
import com.min.meow.post.domain.response.GetBoastCatPostResponse;
import com.min.meow.post.domain.response.CreateBoastCatPostResponse;
import com.min.meow.post.domain.response.UpdateBoastCatPostResponse;
import com.min.meow.post.domain.request.CreateBoastCatPostRequest;
import com.min.meow.post.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.service.BoastCatPostService;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class BoastCatPostServiceImpl implements BoastCatPostService {
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;


    // 모든 글 조회
    @Override
    @Transactional
    @Cacheable(value = "post", key = "'getAllBoastCatPost:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<GetBoastCatPostResponse> getAllBoastCatPosts(Pageable pageable){

        List<BoastCatPost> posts = boastCatPostRepository.findAllWithImageUrls();

        List<GetBoastCatPostResponse> responses = posts.stream()
                .map(GetBoastCatPostResponse::convertToResponse)
                .toList();

        Page<GetBoastCatPostResponse> pageResponses = new PageImpl<>(
                responses,
                pageable,
                responses.size()
        );

        return PageResponse.from(pageResponses);
    }

    // 글 상세 조회
    @Override
    @Transactional
    public GetBoastCatPostResponse getBoastCatPost(Long boastCatPostId){
        BoastCatPost boastCatPost = boastCatPostRepository.findByIdWithImages(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        boastCatPost.plusView();
        return GetBoastCatPostResponse.convertToResponse(boastCatPost);
    }

    // 글 작성
    @Override
    @Transactional
    @CacheEvict(value = "post", allEntries = true)
    public CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, String loginId){

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        List<String> imageUrls = new ArrayList<>();
        if (createBoastCatPostRequest.getImages() != null) {
            imageUrls = s3Uploader.uploadFiles(createBoastCatPostRequest.getImages());
        }

        BoastCatPost boastCatPost = BoastCatPost.convertToEntity(createBoastCatPostRequest, imageUrls, writer);

        boastCatPostRepository.save(boastCatPost);

        return CreateBoastCatPostResponse.convertToDto(boastCatPost);
    }

    // 글 수정
    @Override
    @Transactional
    @CacheEvict(value = "post", allEntries = true)
    public UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest, Long boastCatPostId, String loginId){

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        if(!boastCatPost.getUser().getLoginId().equals(loginId)){
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        boastCatPost.update(updateBoastCatPostRequest);

        return UpdateBoastCatPostResponse.convertToResponse(boastCatPost);
    }

    // 글 삭제
    @Override
    @Transactional
    @CacheEvict(value = "post", allEntries = true)
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
