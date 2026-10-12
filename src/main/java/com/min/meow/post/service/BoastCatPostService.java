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
import com.min.meow.notification.event.PostDeletedEvent;
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
    private final PostImageDeleteEventPublisher postImageDeleteEventPublisher;
    private final NotificationEventPublisher notificationEventPublisher;

    public PageResponse<BoastCatPostListResponse> getAllBoastCatPosts(Pageable pageable){
        long total = boastCatPostRepository.countAllPosts();
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

    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, Long userId){
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        List<String> imageUrls = new ArrayList<>();
        if (createBoastCatPostRequest.getImageKeys() != null && !createBoastCatPostRequest.getImageKeys().isEmpty()) {
            imageUrls = s3Service.toCloudFrontUrls(createBoastCatPostRequest.getImageKeys());
        }

        String videoUrl = createBoastCatPostRequest.getVideoKey() != null
                ? s3Service.toCloudFrontUrl(createBoastCatPostRequest.getVideoKey())
                : null;
        String thumbnailUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);
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
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", boastCatPostId)));
        // 본인이 아니고 관리자 권한(post:delete)도 없으면 → 403
        if (!boastCatPost.isAuthor(userId) && !hasDeleteAuthority) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        List<String> keys = new ArrayList<>(boastCatPost.getImageUrls().stream()
                .map(s3Service::extractKeyFromUrl)
                .filter(key -> key != null && !key.isEmpty())
                .toList());
        if (boastCatPost.getVideoUrl() != null) {
            keys.add(s3Service.extractKeyFromUrl(boastCatPost.getVideoUrl()));
        }
        commentRepository.deleteAllByPostIdAndPostType(boastCatPostId, PostType.BOAST);
        boastCatPostRepository.deleteById(boastCatPostId);
        notificationEventPublisher.publishPostDeletedEvent(new PostDeletedEvent(boastCatPostId));
        postImageDeleteEventPublisher.publish(keys);
    }

    private List<String> updateImage(UpdateBoastCatPostRequest request, BoastCatPost post){
        S3Service.ImageUpdateResult result = s3Service.resolveImageUpdate(request.getImages(), post.getImageUrls());
        postImageDeleteEventPublisher.publish(result.keysToDelete());
        return result.finalUrls();
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

}