package com.min.meow.post.service.impl;

import com.min.meow.config.S3Uploader;
import com.min.meow.global.PageResponse;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.post.service.LostCatPostService;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LostCatPostServiceImpl implements LostCatPostService {

    private final LostCatRepository lostCatRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    // 모든 글 조회 (DB 레벨 페이징)
    // 해당 페이지의 데이터만 DB에서 조회하여 메모리 효율적으로 처리
    @Override
    public PageResponse<GetLostCatPostResponse> getAllLostCatPosts(Pageable pageable){
        Page<LostCatPost> posts = lostCatRepository.findAllWithUser(pageable);

        return PageResponse.from(posts.map(GetLostCatPostResponse::toResponse));
    }

    // 글 상세 조회
    // 조회수 증가는 별도 API로 분리됨
    @Override
    @Transactional(readOnly = true)
    public GetLostCatPostResponse getLostCatPost(Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        return GetLostCatPostResponse.toResponse(lostCatPost);
    }


    // 원자적 쿼리로 동시성 문제 해결
    @Override
    @Transactional
    public void incrementViewCount(Long lostCatPostId) {
        int updatedCount = lostCatRepository.incrementViewCount(lostCatPostId);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
    }

    // 글 생성
    @Override
    @Transactional
    @CacheEvict(value = "mainPage", allEntries = true)
    public CreateLostCatPostResponse createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest, String loginId){
        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        List<String> imageUrls = new ArrayList<>();
        if(createLostCatPostRequest.getImages() != null){
            imageUrls = s3Uploader.uploadFiles(createLostCatPostRequest.getImages());
        }

        LostCatPost lostCatPost = LostCatPost.builder()
                .title(createLostCatPostRequest.getTitle())
                .contents(createLostCatPostRequest.getContent())
                .user(writer)
                .isCompleted(false)
                .lostLocation(createLostCatPostRequest.getLostLocation())
                .latitude(createLostCatPostRequest.getLatitude())
                .longitude(createLostCatPostRequest.getLongitude())
                .catName(createLostCatPostRequest.getCatName())
                .catAge(createLostCatPostRequest.getCatAge())
                .catType(createLostCatPostRequest.getCatType())
                .catWeight(createLostCatPostRequest.getCatWeight())
                .catColor(createLostCatPostRequest.getCatColor())
                .imageUrls(imageUrls)
                .reward(createLostCatPostRequest.getReward())
                .build();
        lostCatRepository.save(lostCatPost);

        return CreateLostCatPostResponse.toResponse(lostCatPost, writer);
    }

    // 글 수정
    @Transactional
    @Override
    @CacheEvict(value = "mainPage", allEntries = true)
    public UpdateLostCatPostResponse updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest, String loginId){
        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        if (!lostCatPost.isAuthor(writer)) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        List<String> finalImageUrls = updateImage(updateLostCatPostRequest);
        lostCatPost.updatePost(
                updateLostCatPostRequest.getTitle(),
                updateLostCatPostRequest.getContent(),
                updateLostCatPostRequest.getCatName(),
                updateLostCatPostRequest.getCatType(),
                updateLostCatPostRequest.getCatColor(),
                updateLostCatPostRequest.getCatAge(),
                updateLostCatPostRequest.getCatWeight(),
                updateLostCatPostRequest.getLostLocation(),
                updateLostCatPostRequest.getLatitude(),
                updateLostCatPostRequest.getLongitude(),
                updateLostCatPostRequest.getReward(),
                finalImageUrls
        );
        return UpdateLostCatPostResponse.toResponse(lostCatPost);
    }

    // 글 삭제
    @Transactional
    @Override
    @CacheEvict(value = "mainPage", allEntries = true)
    public void deleteLostCatPost(Long lostCatPostId, String loginId, String password) {

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        if (!lostCatPost.isAuthor(writer)) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        lostCatRepository.deleteById(lostCatPostId);


        /*
        if (!lostCatRepository.existsById(lostCatPostId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
        lostCatRepository.deleteById(lostCatPostId);
         */
    }

    private List<String> updateImage(UpdateLostCatPostRequest request){
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
    @Cacheable(value = "mainPage", key = "'recentLostPosts'")
    public List<GetLostCatPostResponse> getRecentLostCatPosts() {
        List<LostCatPost> posts = lostCatRepository.findTop20RecentPosts();

        return posts.stream()
                .map(GetLostCatPostResponse::toResponse)
                .toList();
    }
}