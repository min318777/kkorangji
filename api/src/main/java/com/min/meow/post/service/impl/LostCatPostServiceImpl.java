package com.min.meow.post.service.impl;

import com.min.meow.config.S3Uploader;
import com.min.meow.global.PageResponse;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.domain.response.CreateLostCatPostResponse;
import com.min.meow.post.domain.response.GetLostCatPostResponse;
import com.min.meow.post.domain.response.UpdateLostCatPostResponse;
import com.min.meow.post.domain.request.CreateLostCatPostRequest;
import com.min.meow.post.domain.request.UpdateLostCatPostRequest;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.post.service.LostCatPostService;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import jakarta.transaction.Transactional;
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
        lostCatPost.plusView();
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
        LostCatPost lostCatPost = LostCatPost.toEntity(createLostCatPostRequest, imageUrls, writer);
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
        lostCatPost.validateAuthor(writer);

        lostCatPost.update(updateLostCatPostRequest);
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

        lostCatPost.validateAuthor(writer);
        lostCatRepository.deleteById(lostCatPostId);


        /*
        if (!lostCatRepository.existsById(lostCatPostId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
        lostCatRepository.deleteById(lostCatPostId);
         */
    }
}