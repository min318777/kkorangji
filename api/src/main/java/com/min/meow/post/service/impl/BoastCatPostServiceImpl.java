package com.min.meow.post.service.impl;


import com.min.meow.config.S3Uploader;
import com.min.meow.global.PageResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.service.BoastCatPostService;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoastCatPostServiceImpl implements BoastCatPostService {
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    // 모든 글 조회 (DB 레벨 페이징)
    // 해당 페이지의 데이터만 DB에서 조회하여 메모리 효율적으로 처리
    @Override
    public PageResponse<GetBoastCatPostResponse> getAllBoastCatPosts(Pageable pageable){
        Page<BoastCatPost> posts = boastCatPostRepository.findAllWithUser(pageable);

        return PageResponse.from(posts.map(GetBoastCatPostResponse::toResponse));
    }

    // 글 상세 조회
    // 조회수 증가는 원자적 쿼리로 처리하여 동시성 문제 해결
    @Override
    @Transactional
    public GetBoastCatPostResponse getBoastCatPost(Long boastCatPostId){
        // 1. 원자적 조회수 증가 (DB 레벨에서 view = view + 1 수행)
        int updatedCount = boastCatPostRepository.incrementViewCount(boastCatPostId);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }

        // 2. 게시글 조회 (증가된 조회수 포함)
        BoastCatPost boastCatPost = boastCatPostRepository.findByIdWithImages(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        return GetBoastCatPostResponse.toResponse(boastCatPost);
    }

    // 글 작성
    // mainPage 캐시 무효화 (새 글이 메인페이지 최근글 목록에 반영되어야 함)
    @Override
    @Transactional
    @CacheEvict(value = "mainPage", allEntries = true)
    public CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, String loginId){

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        List<String> imageUrls = new ArrayList<>();
        if (createBoastCatPostRequest.getImages() != null) {
            imageUrls = s3Uploader.uploadFiles(createBoastCatPostRequest.getImages());
        }

        // 엔티티 직접 생성 (toEntity 메서드 제거됨)
        BoastCatPost boastCatPost = BoastCatPost.builder()
                .title(createBoastCatPostRequest.getTitle())
                .contents(createBoastCatPostRequest.getContent())
                .imageUrls(imageUrls)
                .user(writer)
                .build();
        boastCatPostRepository.save(boastCatPost);

        return CreateBoastCatPostResponse.toResponse(boastCatPost);
    }

    // 글 수정
    // mainPage 캐시 무효화 (수정된 내용이 메인페이지 최근글 목록에 반영되어야 함)
    @Override
    @Transactional
    @CacheEvict(value = "mainPage", allEntries = true)
    public UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest, Long boastCatPostId, String loginId){

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        if (!boastCatPost.isAuthor(writer)) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        List<String> finalImageUrls = updateImage(updateBoastCatPostRequest);
        // 업데이트 메서드 파라미터 변경됨
        boastCatPost.updatePost(
                updateBoastCatPostRequest.getTitle(),
                updateBoastCatPostRequest.getContent(),
                finalImageUrls
        );

        return UpdateBoastCatPostResponse.convertToResponse(boastCatPost);
    }

    // 글 삭제
    // mainPage 캐시 무효화 (삭제된 글이 메인페이지 최근글 목록에서 제거되어야 함)
    @Override
    @Transactional
    @CacheEvict(value = "mainPage", allEntries = true)
    public void deleteBoastCatPost(Long boastCatPostId, String loginId, String password){
        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 작성자 검증 (validateAuthor 메서드 제거됨)
        if (!boastCatPost.isAuthor(writer)) {
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

    private List<String> updateImage(UpdateBoastCatPostRequest request){
        List<String> finalImageUrls = new ArrayList<>();
        // 1. 유지할 기존 이미지 추가
        if (request.getKeepImageUrls() != null && !request.getKeepImageUrls().isEmpty()){
            finalImageUrls.addAll(request.getKeepImageUrls());
        }
        // 2. 새로운 이미지 업로드 후 추가
        if (request.getNewImages() != null && !request.getNewImages().isEmpty()){
            List<String> newUploadedUrls = s3Uploader.uploadFiles(request.getNewImages());
            finalImageUrls.addAll(newUploadedUrls);
        }
        // 3. 삭제할 이미지 처리 (선택사항: S3에서 실제 파일 삭제)
        // if (request.getDeleteImageUrls() != null && !request.getDeleteImageUrls().isEmpty()){
        //     s3Uploader.deleteFiles(request.getDeleteImageUrls());
        // }
        return finalImageUrls;
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "mainPage", key = "'recentBoastPosts'")
    public List<GetBoastCatPostResponse> getRecentBoastCatPosts() {
        log.info("DB에서 최근 자랑글 20개 조회 (캐시 미스시 DB 직접 조회)");

        List<BoastCatPost> posts = boastCatPostRepository.findTop20RecentPosts();

        return posts.stream()
                .map(GetBoastCatPostResponse::toResponse)
                .toList();
    }
}
