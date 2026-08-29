package com.min.meow.post.service;

import com.min.meow.config.S3Service;
import com.min.meow.common.PageResponse;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.ImageItemRequest;
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
import lombok.extern.slf4j.Slf4j;

/**
 * 실종 고양이 게시글 서비스
 * Presigned URL 기반 이미지 업로드 방식 적용:
 * - 클라이언트가 S3에 직접 업로드 후 key 전달
 * - 서버는 key를 CloudFront URL로 변환하여 저장
 * - 서버 트래픽 비용 절감 및 보안 강화
 */
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
    private final ViewCountService viewCountService;
    private final LostCatPostCountCacheService countCacheService;
    private final PostImageDeleteEventPublisher postImageDeleteEventPublisher;

    // ========== 조회 ==========

    /**
     * 모든 실종 고양이 게시글 목록 조회 (Projection 적용으로 성능 최적화)
     * 성능 개선 내역:
     * - Before: Entity 전체 조회 + DTO 변환 → contents, imageUrls, comments 모두 조회
     *           → LazyInitializationException 발생 가능
     * - After: Projection으로 필요한 10개 컬럼만 SELECT
     *          (id, title, writer(nickname), catName, lostLocation, commentCount, view, isCompleted, createdAt, thumbnailUrl)
     */
    public PageResponse<LostCatPostListResponse> getAllLostCatPosts(Pageable pageable){
        // COUNT는 캐시에서, content는 커버링 인덱스 서브쿼리로 조회
        long total = countCacheService.countAll();
        Page<LostCatPostListResponse> posts = new PageImpl<>(
                lostCatRepository.findContentWithCoveringIndexUsingIn(pageable),
                pageable,
                total
        );
        return PageResponse.from(posts);
    }

    /**
     * 상세조회 + 조회수 증가 통합 (v3) — Redis INCR 후 상세조회 반환
     * 조회수는 배치 동기화(30초 주기) 전까지 DB 값 그대로 응답에 실림
     */
    public GetLostCatPostResponse getLostCatPostV3(Long lostCatPostId, String identifier){
        GetLostCatPostResponse response = getLostCatPost(lostCatPostId);
        viewCountService.incrementViewCount(PostType.LOST, lostCatPostId, identifier);
        return response;
    }

    /**
     * 글 상세 조회 (N+1 최적화 적용, 캐싱 없음)
     * findByIdWithUser()로 User를 Fetch Join하여 N+1 문제 해결
     * - User: Fetch Join (N:1 관계 → 카테시안 곱 없음)
     */

    public GetLostCatPostResponse getLostCatPost(Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findByIdWithUser(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        return GetLostCatPostResponse.builder()
                .id(lostCatPost.getId())
                .title(lostCatPost.getTitle())
                .content(lostCatPost.getContents())
                .writer(lostCatPost.getUser().getNickname())
                .userId(lostCatPost.getUser().getId())
                .catName(lostCatPost.getCatName())
                .catType(lostCatPost.getCatType())
                .catColor(lostCatPost.getCatColor())
                .catAge(lostCatPost.getCatAge())
                .catWeight(lostCatPost.getCatWeight())
                .catGender(lostCatPost.getCatGender())
                .lostDate(lostCatPost.getLostDate())
                .imageUrls(new ArrayList<>(lostCatPost.getImageUrls()))
                .lostLocation(lostCatPost.getLostLocation())
                .reward(lostCatPost.getReward())
                .latitude(lostCatPost.getLatitude())
                .longitude(lostCatPost.getLongitude())
                .commentCount(lostCatPost.getCommentCount())
                .view(lostCatPost.getView())
                .completed(lostCatPost.isCompleted())
                .createdAt(lostCatPost.getCreatedAt())
                .updatedAt(lostCatPost.getUpdatedAt())
                .build();
    }

    /**
     * 내 주변 실종글 조회 (Bounding Box 필터)
     * 프론트에서 전달한 현재 위치(lat, lng)를 기준으로
     * radiusKm 반경 내 실종글만 페이징 조회합니다.
     * latitude/longitude가 없는 게시글은 자동으로 제외됩니다.
     */
    public PageResponse<LostCatPostListResponse> getNearbyLostCatPosts(double lat, double lng, double radiusKm, Pageable pageable) {
        Page<LostCatPostListResponse> posts = lostCatRepository.findNearbyWithProjection(lat, lng, radiusKm, pageable);
        return PageResponse.from(posts);
    }

    // ST_Distance_Sphere 방식 — 정확한 원형 반경, 가까운 순 정렬
    public PageResponse<LostCatPostListResponse> getNearbyLostCatPostsST(double lat, double lng, double radiusKm, Pageable pageable) {
        Page<LostCatPostListResponse> posts = lostCatRepository.findNearbyWithST(lat, lng, radiusKm, pageable);
        return PageResponse.from(posts);
    }

    // ========== CRUD ==========

    /**
     * 글 작성 (Presigned URL 기반 이미지 업로드)
     * 이미지 업로드 플로우:
     * 1. 클라이언트가 /api/images/presigned-urls 로 Presigned URL 요청
     * 2. 클라이언트가 Presigned URL로 S3에 이미지 직접 업로드
     * 3. 업로드 완료 후 받은 S3 key를 imageKeys에 담아서 이 API 호출
     * 4. 서버는 key를 CloudFront URL로 변환하여 DB 저장
     */
    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public CreateLostCatPostResponse createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest, Long userId){
        countCacheService.evict();
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // S3 key를 CloudFront URL로 변환
        List<String> imageUrls = new ArrayList<>();
        if (createLostCatPostRequest.getImageKeys() != null && !createLostCatPostRequest.getImageKeys().isEmpty()) {
            imageUrls = s3Service.toCloudFrontUrls(createLostCatPostRequest.getImageKeys());
        }

        // 첫 번째 이미지를 썸네일로 저장 (목록 조회 시 JOIN 없이 사용)
        String thumbnailUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

        // latitude/longitude → POINT 변환 (SPATIAL INDEX 활용)
        Double latitude = createLostCatPostRequest.getLatitude();
        Double longitude = createLostCatPostRequest.getLongitude();
        Point location = (latitude != null && longitude != null)
                ? GEO_FACTORY.createPoint(new Coordinate(longitude, latitude)) : null;

        // 엔티티 생성
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

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     * 이미지 처리:
     * - images 리스트 순서 그대로 최종 이미지 목록 구성 (기존 URL + 새 S3 key 혼합 가능)
     * - images에 없는 기존 이미지는 삭제된 것으로 간주하여 S3에서 제거
     */
    @Transactional
    public UpdateLostCatPostResponse updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest, Long userId){
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 수정은 본인만 허용 (관리자도 타인 글 수정 불가)
        if (!lostCatPost.isAuthor(writer)) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        List<String> finalImageUrls = updateImage(updateLostCatPostRequest, lostCatPost);

        Double latitude = updateLostCatPostRequest.getLatitude();
        Double longitude = updateLostCatPostRequest.getLongitude();
        Point location = (latitude != null && longitude != null)
                ? GEO_FACTORY.createPoint(new Coordinate(longitude, latitude)) : null;

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

    /**
     * 글 삭제
     */
    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public void deleteLostCatPost(Long lostCatPostId, Long userId, boolean hasDeleteAuthority) {
        countCacheService.evict();
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 본인이 아니고 관리자 권한(post:delete)도 없으면 → 403
        if (!lostCatPost.isAuthor(writer) && !hasDeleteAuthority) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        // S3 이미지 삭제 (DB 삭제 전에 URL 추출)
        List<String> keys = lostCatPost.getImageUrls().stream()
                .map(s3Service::extractKeyFromUrl)
                .filter(key -> key != null && !key.isEmpty())
                .toList();
        // 연관 댓글 먼저 삭제 (cascade 제거로 인한 수동 처리)
        commentRepository.deleteAllByPostIdAndPostType(lostCatPostId, PostType.LOST);
        lostCatRepository.deleteById(lostCatPostId);
        // S3 삭제는 트랜잭션 커밋 후 비동기로 처리 (외부 API 호출을 트랜잭션 밖으로 분리)
        postImageDeleteEventPublisher.publish(keys);
        log.info("게시글 삭제 완료 - userId: {}, postId: {}", userId, lostCatPostId);
    }

    /**
     * 실종 상태 변경 (찾는 중 ↔ 귀가 완료)
     * 본인 게시글만 변경 가능. 목록 캐시 무효화.
     */
    @Transactional
    public void updateCompletedStatus(Long lostCatPostId, boolean isCompleted, Long userId) {
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        LostCatPost post = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        if (!post.isAuthor(writer)) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        post.setCompletedStatus(isCompleted);
        lostCatRepository.save(post);
    }

    /**
     * 이미지 업데이트 처리 (Presigned URL 방식)
     * images 리스트 순서 그대로 최종 이미지 목록을 구성하고,
     * 기존 이미지 중 최종 목록에서 빠진 것은 S3에서 삭제한다.
     * @param request 수정 요청 DTO
     * @param post 수정 대상 게시글 (삭제 대상 판별용)
     * @return 최종 이미지 URL 목록 (CloudFront URL)
     */
    private List<String> updateImage(UpdateLostCatPostRequest request, LostCatPost post){
        List<String> finalImageUrls = new ArrayList<>();

        if (request.getImages() != null) {
            for (ImageItemRequest image : request.getImages()) {
                finalImageUrls.add(image.getType() == ImageItemRequest.ImageType.NEW
                        ? s3Service.toCloudFrontUrl(image.getValue())
                        : image.getValue());
            }
        }

        // 기존 이미지 중 최종 목록에 없는 것은 삭제된 것으로 간주하여 S3에서 제거
        // S3 삭제는 트랜잭션 커밋 후 비동기로 처리 (외부 API 호출을 트랜잭션 밖으로 분리)
        List<String> keysToDelete = post.getImageUrls().stream()
                .filter(url -> !finalImageUrls.contains(url))
                .map(s3Service::extractKeyFromUrl)
                .toList();
        postImageDeleteEventPublisher.publish(keysToDelete);

        return finalImageUrls;
    }

    // ========== 조회수 ==========

    /**
     * 조회수 증가 - 더티 체킹 방식 (v1 - 동시성 이슈 있음)
     * 동시성 문제 (Lost Update):
     * 이 방식은 아래와 같은 Read-Modify-Write 패턴으로 동작합니다:
     * 1. SELECT * FROM lost_cat_post WHERE id = ? (조회)
     * 2. Java에서 view++ 연산 수행
     * 3. UPDATE lost_cat_post SET view = 101 WHERE id = ? (절대값으로 UPDATE)
     * 문제 시나리오 (현재 view = 100, 동시 2개 요청):
     * - Thread A: view 읽기 (100) → view++ → 101로 UPDATE
     * - Thread B: view 읽기 (100) → view++ → 101로 UPDATE (동시에!)
     * - 결과: 2번 증가 요청 → 실제 1만 증가 (Lost Update)
     * K6 동시성 테스트로 이 문제를 발견하여
     * incrementViewCount() 원자적 쿼리 방식으로 개선하였습니다.
     * @deprecated 동시성 이슈로 인해 incrementViewCount() 사용 권장
     */
    @Deprecated
    @Transactional
    public void incrementViewCountWithDirtyChecking(Long lostCatPostId) {
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        lostCatPost.incrementView();
        // 트랜잭션 종료 시 JPA가 변경 감지하여 UPDATE 쿼리 실행
    }

    /**
     * 조회수 증가 - 원자적 쿼리 방식 (v2 - 개선된 버전)
     * DB 레벨에서 view = view + 1을 수행하여 Race Condition을 방지합니다.
     * 여러 스레드가 동시에 호출해도 정확한 조회수가 보장됩니다.
     * 실행되는 쿼리:
     * UPDATE lost_cat_post SET view = view + 1 WHERE id = ?
     * 이 쿼리는 DB 레벨에서 원자적으로 실행되므로:
     * - Read-Modify-Write 패턴이 아님
     * - 동시 요청 시에도 모든 증가가 정확히 반영됨
     * K6 동시성 테스트 결과:
     * - 더티 체킹 방식: 1000 VU 동시 요청 → 약 800~900 증가 (Lost Update)
     * - 원자적 쿼리: 1000 VU 동시 요청 → 정확히 1000 증가 ✅
     */
    @Transactional
    public void incrementViewCount(Long lostCatPostId) {
        int updatedCount = lostCatRepository.incrementViewCount(lostCatPostId);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
    }
}
