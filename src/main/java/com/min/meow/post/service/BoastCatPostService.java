package com.min.meow.post.service;


import com.min.meow.config.S3Service;
import com.min.meow.common.PageResponse;
import com.min.meow.common.SecurityUtil;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.ImageItemRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.notification.event.NotificationEventPublisher;
import com.min.meow.notification.event.PopularScoreEvent;
import com.min.meow.post.event.PostImageDeleteEventPublisher;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.min.meow.common.PostType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoastCatPostService {
    private final BoastCatPostRepository boastCatPostRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final BoastCatPostCountCacheService countCacheService;
    private final PopularRankingService popularRankingService;
    private final PostImageDeleteEventPublisher postImageDeleteEventPublisher;
    private final NotificationEventPublisher notificationEventPublisher;


    // 모든 글 조회 (Projection 적용으로 성능 최적화)
    public PageResponse<BoastCatPostListResponse> getAllBoastCatPosts(Pageable pageable){
        // COUNT는 캐시에서, content는 DB에서 조회 (COUNT 쿼리 성능 개선)
        long total = countCacheService.countAll();
        Page<BoastCatPostListResponse> posts = new PageImpl<>(
                boastCatPostRepository.findContentWithCoveringIndexUsingIn(pageable),
                pageable,
                total
        );
        return PageResponse.from(posts);
    }

    // 자랑글 상세 조회
    public GetBoastCatPostResponse getBoastCatPost(Long boastCatPostId){
        BoastCatPost post = boastCatPostRepository.findByIdWithUser(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", boastCatPostId)));
        return GetBoastCatPostResponse.from(post);
    }

    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, Long userId){
        countCacheService.evict();

        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // S3 key를 CloudFront URL로 변환
        List<String> imageUrls = new ArrayList<>();
        if (createBoastCatPostRequest.getImageKeys() != null && !createBoastCatPostRequest.getImageKeys().isEmpty()) {
            imageUrls = s3Service.toCloudFrontUrls(createBoastCatPostRequest.getImageKeys());
        }

        String videoUrl = createBoastCatPostRequest.getVideoKey() != null
                ? s3Service.toCloudFrontUrl(createBoastCatPostRequest.getVideoKey())
                : null;

        // 첫 번째 이미지를 썸네일로 저장 (목록 조회 시 JOIN 없이 사용)
        String thumbnailUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

        // 엔티티 생성
        BoastCatPost boastCatPost = BoastCatPost.builder()
                .title(createBoastCatPostRequest.getTitle())
                .contents(createBoastCatPostRequest.getContent())
                .imageUrls(imageUrls)
                .videoUrl(videoUrl)
                .thumbnailUrl(thumbnailUrl)
                .user(writer)
                .build();
        boastCatPostRepository.save(boastCatPost);

        log.info("게시글 작성 완료 - userId: {}, postId: {}", userId, boastCatPost.getId());
        return CreateBoastCatPostResponse.from(boastCatPost);
    }

    // 글 수정
    @Transactional
    public UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest, Long boastCatPostId, Long userId){

        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", boastCatPostId)));

        // 본인이 아니고 관리자 권한(post:update)도 없으면 → 403
        if (!boastCatPost.isAuthor(userId) && !SecurityUtil.hasAuthority("post:update")) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        List<String> finalImageUrls = updateImage(updateBoastCatPostRequest, boastCatPost);
        String finalVideoUrl = updateVideo(updateBoastCatPostRequest, boastCatPost);
        boastCatPost.updatePost(
                updateBoastCatPostRequest.getTitle(),
                updateBoastCatPostRequest.getContent(),
                finalImageUrls,
                finalVideoUrl
        );

        return UpdateBoastCatPostResponse.from(boastCatPost);
    }

    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public void deleteBoastCatPost(Long boastCatPostId, Long userId, boolean hasDeleteAuthority){
        countCacheService.evict();
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", boastCatPostId)));
        // 본인이 아니고 관리자 권한(post:delete)도 없으면 → 403
        if (!boastCatPost.isAuthor(userId) && !hasDeleteAuthority) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        // S3 이미지/동영상 삭제 (DB 삭제 전에 URL 추출)
        List<String> keys = new ArrayList<>(boastCatPost.getImageUrls().stream()
                .map(s3Service::extractKeyFromUrl)
                .filter(key -> key != null && !key.isEmpty())
                .toList());
        if (boastCatPost.getVideoUrl() != null) {
            keys.add(s3Service.extractKeyFromUrl(boastCatPost.getVideoUrl()));
        }
        // 연관 댓글 먼저 삭제 (cascade 제거로 인한 수동 처리)
        commentRepository.deleteAllByPostIdAndPostType(boastCatPostId, PostType.BOAST);
        boastCatPostRepository.deleteById(boastCatPostId);
        popularRankingService.removeFromRanking(boastCatPostId);
        // S3 삭제는 트랜잭션 커밋 후 비동기로 처리 (외부 API 호출을 트랜잭션 밖으로 분리)
        postImageDeleteEventPublisher.publish(keys);
    }

    // 이미지 업데이트 처리 — images 순서대로 최종 목록 구성, 빠진 기존 이미지는 S3에서 삭제
    private List<String> updateImage(UpdateBoastCatPostRequest request, BoastCatPost post){
        List<String> finalImageUrls = new ArrayList<>();

        if (request.getImages() != null) {
            for (ImageItemRequest image : request.getImages()) {
                finalImageUrls.add(image.getType() == ImageItemRequest.ImageType.NEW
                        ? s3Service.toCloudFrontUrl(image.getValue())
                        : image.getValue());
            }
        }
        // S3 삭제는 트랜잭션 커밋 후 비동기로 처리
        List<String> keysToDelete = post.getImageUrls().stream()
                .filter(url -> !finalImageUrls.contains(url))
                .map(s3Service::extractKeyFromUrl)
                .toList();
        postImageDeleteEventPublisher.publish(keysToDelete);

        return finalImageUrls;
    }

    // 동영상 업데이트 처리
    private String updateVideo(UpdateBoastCatPostRequest request, BoastCatPost post) {
        ImageItemRequest video = request.getVideo();
        String existingVideoUrl = post.getVideoUrl();

        if (video == null) {
            return existingVideoUrl;
        }

        String finalVideoUrl = switch (video.getType()) {
            case EXISTING -> existingVideoUrl;
            case NEW -> s3Service.toCloudFrontUrl(video.getValue());
            case REMOVE -> null;
        };

        if (existingVideoUrl != null && !existingVideoUrl.equals(finalVideoUrl)) {
            postImageDeleteEventPublisher.publish(List.of(s3Service.extractKeyFromUrl(existingVideoUrl)));
        }

        return finalVideoUrl;
    }

    // ========== 상세조회 + 조회수 통합 API ==========

    // v1: 상세조회 + 더티 체킹
    @Transactional
    public GetBoastCatPostResponse getBoastCatPostV1(Long id) {
        BoastCatPost post = boastCatPostRepository.findByIdWithUser(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", id)));
        post.incrementView();
        return GetBoastCatPostResponse.from(post);
    }

    // v2: 상세조회 + 원자적 UPDATE
    @Transactional
    public GetBoastCatPostResponse getBoastCatPostV2(Long id) {
        BoastCatPost post = boastCatPostRepository.findByIdWithUser(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", id)));
        boastCatPostRepository.incrementViewCount(id);
        // 인기글 Sorted Set 점수 +1 (AFTER_COMMIT 비동기 처리)
        notificationEventPublisher.publishPopularScoreEvent(new PopularScoreEvent(id, 1));
        return GetBoastCatPostResponse.from(post);
    }

    // v3: 상세조회 + 비관적 락
    @Transactional
    public GetBoastCatPostResponse getBoastCatPostV3(Long id) {
        BoastCatPost post = boastCatPostRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", id)));
        post.incrementView();
        return GetBoastCatPostResponse.from(post);
    }

}