package com.min.meow.post.service;

import com.min.meow.config.S3Service;
import com.min.meow.common.PageResponse;
import com.min.meow.common.SecurityUtil;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.common.PostType;
import com.min.meow.post.event.PostImageDeleteEventPublisher;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostCatPostService {

    private static final GeometryFactory GEO_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final LostCatRepository lostCatRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final PostImageDeleteEventPublisher postImageDeleteEventPublisher;

    public PageResponse<LostCatPostListResponse> getAllLostCatPosts(Pageable pageable){
        long total = lostCatRepository.countAllPosts();
        Page<LostCatPostListResponse> posts = new PageImpl<>(
                lostCatRepository.findContentWithCoveringIndexUsingIn(pageable),
                pageable,
                total
        );
        return PageResponse.from(posts);
    }

    public GetLostCatPostResponse getLostCatPost(Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findByIdWithUser(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", lostCatPostId)));

        return GetLostCatPostResponse.from(lostCatPost);
    }

    // ========== 상세조회 + 조회수 통합 API ==========

    @Transactional
    public GetLostCatPostResponse getLostCatPostV2(Long lostCatPostId) {
        GetLostCatPostResponse response = getLostCatPost(lostCatPostId);
        lostCatRepository.incrementViewCount(lostCatPostId);
        return response;
    }

    // 내 주변 실종글 조회(Bounding Box 필터) — 현재 위치 기준 radiusKm 반경 내 실종글만 페이징 조회, 좌표 없는 글은 자동 제외
    public PageResponse<LostCatPostListResponse> getNearbyLostCatPosts(double lat, double lng, double radiusKm, Pageable pageable) {
        Page<LostCatPostListResponse> posts = lostCatRepository.findNearbyWithProjection(lat, lng, radiusKm, pageable);
        return PageResponse.from(posts);
    }

    // ST_Distance_Sphere 방식 — 정확한 원형 반경, 가까운 순 정렬
    public PageResponse<LostCatPostListResponse> getNearbyLostCatPostsST(double lat, double lng, double radiusKm, Pageable pageable) {
        Page<LostCatPostListResponse> posts = lostCatRepository.findNearbyWithST(lat, lng, radiusKm, pageable);
        return PageResponse.from(posts);
    }


    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public CreateLostCatPostResponse createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest, Long userId){
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // S3 key를 CloudFront URL로 변환
        List<String> imageUrls = new ArrayList<>();
        if (createLostCatPostRequest.getImageKeys() != null && !createLostCatPostRequest.getImageKeys().isEmpty()) {
            imageUrls = s3Service.toCloudFrontUrls(createLostCatPostRequest.getImageKeys());
        }

        // 첫 번째 이미지를 썸네일로 저장 (목록 조회 시 JOIN 없이 사용)
        String thumbnailUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

        Double latitude = createLostCatPostRequest.getLatitude();
        Double longitude = createLostCatPostRequest.getLongitude();
        Point location = toPoint(latitude, longitude);

        LostCatPost lostCatPost = LostCatPost.builder()
                .title(createLostCatPostRequest.getTitle())
                .contents(createLostCatPostRequest.getContent())
                .user(writer)
                .isCompleted(false)
                .lostLocation(createLostCatPostRequest.getLostLocation())
                .latitude(latitude)
                .longitude(longitude)
                .location(location)
                .catName(createLostCatPostRequest.getCatName())
                .catAge(createLostCatPostRequest.getCatAge())
                .catType(createLostCatPostRequest.getCatType())
                .catWeight(createLostCatPostRequest.getCatWeight())
                .catColor(createLostCatPostRequest.getCatColor())
                .catGender(createLostCatPostRequest.getCatGender())
                .lostDate(createLostCatPostRequest.getLostDate())
                .imageUrls(imageUrls)
                .thumbnailUrl(thumbnailUrl)
                .reward(createLostCatPostRequest.getReward())
                .build();
        lostCatRepository.save(lostCatPost);

        return CreateLostCatPostResponse.from(lostCatPost);
    }

    @Transactional
    public UpdateLostCatPostResponse updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest, Long userId){
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", lostCatPostId)));

        // 본인이 아니고 관리자 권한(post:update)도 없으면 → 403
        if (!lostCatPost.isAuthor(userId) && !SecurityUtil.hasAuthority("post:update")) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        List<String> finalImageUrls = updateImage(updateLostCatPostRequest, lostCatPost);

        Double latitude = updateLostCatPostRequest.getLatitude();
        Double longitude = updateLostCatPostRequest.getLongitude();
        Point location = toPoint(latitude, longitude);

        lostCatPost.updatePost(
                updateLostCatPostRequest.getTitle(),
                updateLostCatPostRequest.getContent(),
                updateLostCatPostRequest.getCatName(),
                updateLostCatPostRequest.getCatType(),
                updateLostCatPostRequest.getCatColor(),
                updateLostCatPostRequest.getCatAge(),
                updateLostCatPostRequest.getCatWeight(),
                updateLostCatPostRequest.getCatGender(),
                updateLostCatPostRequest.getLostDate(),
                updateLostCatPostRequest.getLostLocation(),
                latitude,
                longitude,
                location,
                updateLostCatPostRequest.getReward(),
                finalImageUrls
        );
        return UpdateLostCatPostResponse.from(lostCatPost);
    }

    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public void deleteLostCatPost(Long lostCatPostId, Long userId, boolean hasDeleteAuthority) {
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", lostCatPostId)));

        // 본인이 아니고 관리자 권한(post:delete)도 없으면 → 403
        if (!lostCatPost.isAuthor(userId) && !hasDeleteAuthority) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        List<String> keys = lostCatPost.getImageUrls().stream()
                .map(s3Service::extractKeyFromUrl)
                .filter(key -> key != null && !key.isEmpty())
                .toList();
        commentRepository.deleteAllByPostIdAndPostType(lostCatPostId, PostType.LOST);
        lostCatRepository.deleteById(lostCatPostId);
        postImageDeleteEventPublisher.publish(keys);
        log.info("게시글 삭제 완료 - userId: {}, postId: {}", userId, lostCatPostId);
    }

    // 실종 상태 변경 (찾는 중 ↔ 귀가 완료) — 본인 게시글만 변경 가능
    @Transactional
    public void updateCompletedStatus(Long lostCatPostId, boolean isCompleted, Long userId) {
        LostCatPost post = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST, Map.of("postId", lostCatPostId)));
        if (!post.isAuthor(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        post.setCompletedStatus(isCompleted);
        lostCatRepository.save(post);
    }

    // latitude/longitude → POINT 변환 (SPATIAL INDEX 활용)
    private Point toPoint(Double latitude, Double longitude) {
        return (latitude != null && longitude != null)
                ? GEO_FACTORY.createPoint(new Coordinate(longitude, latitude)) : null;
    }

    // 이미지 업데이트 처리
    private List<String> updateImage(UpdateLostCatPostRequest request, LostCatPost post){
        S3Service.ImageUpdateResult result = s3Service.resolveImageUpdate(request.getImages(), post.getImageUrls());
        postImageDeleteEventPublisher.publish(result.keysToDelete());
        return result.finalUrls();
    }
}
