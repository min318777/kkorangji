package com.min.meow.post.service.impl;

import com.min.meow.config.S3Service;
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

/**
 * 실종 고양이 게시글 서비스 구현체
 *
 * Presigned URL 기반 이미지 업로드 방식 적용:
 * - 클라이언트가 S3에 직접 업로드 후 key 전달ㄴ
 * - 서버는 key를 CloudFront URL로 변환하여 저장
 * - 서버 트래픽 비용 절감 및 보안 강화
 */
@Service
@RequiredArgsConstructor
public class LostCatPostServiceImpl implements LostCatPostService {

    private final LostCatRepository lostCatRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    // 모든 글 조회 (DB 레벨 페이징)
    // 해당 페이지의 데이터만 DB에서 조회하여 메모리 효율적으로 처리
    @Override
    public PageResponse<GetLostCatPostResponse> getAllLostCatPosts(Pageable pageable){
        Page<LostCatPost> posts = lostCatRepository.findAllWithUser(pageable);

        return PageResponse.from(posts.map(GetLostCatPostResponse::toResponse));
    }

    /**
     * 글 상세 조회 (N+1 최적화 적용)
     * findByIdWithUser()로 User를 Fetch Join하여 N+1 문제 해결
     * - User: Fetch Join (N:1 관계 → 카테시안 곱 없음)
     * - imageUrls: @BatchSize(100) 적용 (1:N 관계)
     * - comments: @BatchSize(100) 적용 (1:N 관계)
     * 조회수 증가는 별도 API로 분리됨
     */
    @Override
    @Transactional(readOnly = true)
    public GetLostCatPostResponse getLostCatPost(Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findByIdWithUser(lostCatPostId)
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

    /**
     * 글 작성 (Presigned URL 기반 이미지 업로드)
     *
     * 이미지 업로드 플로우:
     * 1. 클라이언트가 /api/images/presigned-urls 로 Presigned URL 요청
     * 2. 클라이언트가 Presigned URL로 S3에 이미지 직접 업로드
     * 3. 업로드 완료 후 받은 S3 key를 imageKeys에 담아서 이 API 호출
     * 4. 서버는 key를 CloudFront URL로 변환하여 DB 저장
     *
     * mainPage 캐시 무효화 (새 글이 메인페이지 최근글 목록에 반영되어야 함)
     */
    @Override
    @Transactional
    @CacheEvict(value = "mainPage", allEntries = true)
    public CreateLostCatPostResponse createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest, String loginId){
        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // S3 key를 CloudFront URL로 변환
        List<String> imageUrls = new ArrayList<>();
        if (createLostCatPostRequest.getImageKeys() != null && !createLostCatPostRequest.getImageKeys().isEmpty()) {
            imageUrls = s3Service.toCloudFrontUrls(createLostCatPostRequest.getImageKeys());
        }

        // 엔티티 생성
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

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     *
     * 이미지 처리:
     * - 새 이미지: newImageKeys의 S3 key를 CloudFront URL로 변환
     * - 유지할 이미지: keepImageUrls 그대로 사용
     * - 삭제할 이미지: deleteImageUrls의 URL에서 S3 key 추출 후 삭제
     *
     * mainPage 캐시 무효화 (수정된 내용이 메인페이지 최근글 목록에 반영되어야 함)
     */
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

    /**
     * 이미지 업데이트 처리 (Presigned URL 방식)
     *
     * @param request 수정 요청 DTO
     * @return 최종 이미지 URL 목록 (CloudFront URL)
     */
    private List<String> updateImage(UpdateLostCatPostRequest request){
        List<String> finalImageUrls = new ArrayList<>();

        // 1. 유지할 기존 이미지 추가 (CloudFront URL 그대로)
        if (request.getKeepImageUrls() != null && !request.getKeepImageUrls().isEmpty()){
            finalImageUrls.addAll(request.getKeepImageUrls());
        }

        // 2. 새로운 이미지 URL 추가 (S3 key → CloudFront URL 변환)
        if (request.getNewImageKeys() != null && !request.getNewImageKeys().isEmpty()){
            List<String> newCloudFrontUrls = s3Service.toCloudFrontUrls(request.getNewImageKeys());
            finalImageUrls.addAll(newCloudFrontUrls);
        }

        // 3. 삭제할 이미지 S3에서 제거
        if (request.getDeleteImageUrls() != null && !request.getDeleteImageUrls().isEmpty()){
            // CloudFront URL에서 S3 key 추출 후 삭제
            List<String> keysToDelete = request.getDeleteImageUrls().stream()
                    .map(s3Service::extractKeyFromUrl)
                    .toList();
            s3Service.deleteFiles(keysToDelete);
        }

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