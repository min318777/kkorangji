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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LostCatPostServiceImpl implements LostCatPostService {

    private final LostCatRepository lostCatRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    // 모든 글 조회
    @Override
    @CacheEvict(value = "post", allEntries = true)
    @Cacheable(value = "post", key = "'getAllLostCatPost:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<GetLostCatPostResponse> getAllLostCatPosts(Pageable pageable){

        List<LostCatPost> posts = lostCatRepository.findAllWithImageUrls();
        List<GetLostCatPostResponse> responses = posts.stream()
                .map(GetLostCatPostResponse::toResponse)
                .toList();
        Page<GetLostCatPostResponse> pageResponse = new PageImpl<>(
                responses,
                pageable,
                responses.size()
        );

        // PageResponse로 변환하여 반환 (Redis 직렬화 가능한 형태)
        return PageResponse.from(pageResponse);
    }

    // 글 상세 조회
    @Override
    @Transactional
    @CacheEvict(value = "post", allEntries = true)
    public GetLostCatPostResponse getLostCatPost(Long lostCatPostId){

        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        lostCatPost.increaseView();  // plusView() → increaseView()로 변경
        return GetLostCatPostResponse.toResponse(lostCatPost);
    }

    // 글 생성
    @Override
    @Transactional
    @CacheEvict(value = "post", allEntries = true)
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
    @CacheEvict(value = "post", allEntries = true)
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
    // 성능개선-> findById 이후 delete를 db호출 2번발생-> deleteById 한번의 호출로 성능개성 -> existById도 있는데? -> 존재여부만 확인하므로 엔티티 조회보다 가벼운 호출이다. -> query dsl로 해볼까?
    // 물리적삭제 대신 소프트삭제도 고려해보자.
    @Transactional
    @Override
    @CacheEvict(value = "post", allEntries = true)
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
}