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
    private final ViewCountService viewCountService;
    private final PopularRankingService popularRankingService;
    private final PostImageDeleteEventPublisher postImageDeleteEventPublisher;

    // ========== 조회 ==========

    /**
     * 모든 글 조회 (Projection 적용으로 성능 최적화)
     * 성능 개선 내역:
     * - Before: Entity 전체 조회 + DTO 변환 → contents, imageUrls, comments 모두 조회
     * - After: Projection으로 필요한 7개 컬럼만 SELECT (id, title, likeCount, commentCount, view, createdAt, thumbnailUrl)
     * - 커버링 인덱스로 id만 먼저 조회 후, id 목록으로 IN 절 조회하여 LIMIT 개수만큼만 클러스터링 인덱스에 접근
     */
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

    /** 일반 자랑글 상세 조회 — 캐싱 없음, 단순 DB 조회 */
    public GetBoastCatPostResponse getBoastCatPost(Long boastCatPostId){
        BoastCatPost post = boastCatPostRepository.findByIdWithUser(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        return GetBoastCatPostResponse.from(post);
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
    public CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, Long userId){
        countCacheService.evict();

        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // S3 key를 CloudFront URL로 변환
        List<String> imageUrls = new ArrayList<>();
        if (createBoastCatPostRequest.getImageKeys() != null && !createBoastCatPostRequest.getImageKeys().isEmpty()) {
            imageUrls = s3Service.toCloudFrontUrls(createBoastCatPostRequest.getImageKeys());
        }

        // 첫 번째 이미지를 썸네일로 저장 (목록 조회 시 JOIN 없이 사용)
        String thumbnailUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

        // 엔티티 생성
        BoastCatPost boastCatPost = BoastCatPost.builder()
                .title(createBoastCatPostRequest.getTitle())
                .contents(createBoastCatPostRequest.getContent())
                .imageUrls(imageUrls)
                .thumbnailUrl(thumbnailUrl)
                .user(writer)
                .build();
        boastCatPostRepository.save(boastCatPost);

        log.info("게시글 작성 완료 - userId: {}, postId: {}", userId, boastCatPost.getId());
        return CreateBoastCatPostResponse.from(boastCatPost);
    }

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     * 이미지 처리:
     * - images 리스트 순서 그대로 최종 이미지 목록 구성 (기존 URL + 새 S3 key 혼합 가능)
     * - images에 없는 기존 이미지는 삭제된 것으로 간주하여 S3에서 제거
     */
    @Transactional
    public UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest, Long boastCatPostId, Long userId){

        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 본인이 아니고 관리자 권한(post:update)도 없으면 → 403
        if (!boastCatPost.isAuthor(writer) && !SecurityUtil.hasAuthority("post:update")) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        List<String> finalImageUrls = updateImage(updateBoastCatPostRequest, boastCatPost);
        boastCatPost.updatePost(
                updateBoastCatPostRequest.getTitle(),
                updateBoastCatPostRequest.getContent(),
                finalImageUrls
        );

        return UpdateBoastCatPostResponse.from(boastCatPost);
    }

    /**
     * 글 삭제
     */
    @Transactional
    @CacheEvict(cacheNames = "user:stats", key = "#userId")
    public void deleteBoastCatPost(Long boastCatPostId, Long userId, boolean hasDeleteAuthority){
        countCacheService.evict();
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        // 본인이 아니고 관리자 권한(post:delete)도 없으면 → 403
        if (!boastCatPost.isAuthor(writer) && !hasDeleteAuthority) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        // S3 이미지 삭제 (DB 삭제 전에 URL 추출)
        List<String> keys = boastCatPost.getImageUrls().stream()
                .map(s3Service::extractKeyFromUrl)
                .filter(key -> key != null && !key.isEmpty())
                .toList();
        // 연관 댓글 먼저 삭제 (cascade 제거로 인한 수동 처리)
        commentRepository.deleteAllByPostIdAndPostType(boastCatPostId, PostType.BOAST);
        boastCatPostRepository.deleteById(boastCatPostId);
        popularRankingService.removeFromRanking(boastCatPostId);
        // S3 삭제는 트랜잭션 커밋 후 비동기로 처리 (외부 API 호출을 트랜잭션 밖으로 분리)
        postImageDeleteEventPublisher.publish(keys);
    }

    /**
     * 이미지 업데이트 처리 (Presigned URL 방식)
     * images 리스트 순서 그대로 최종 이미지 목록을 구성하고,
     * 기존 이미지 중 최종 목록에서 빠진 것은 S3에서 삭제한다.
     * @param request 수정 요청 DTO
     * @param post 수정 대상 게시글 (삭제 대상 판별용)
     * @return 최종 이미지 URL 목록 (CloudFront URL)
     */
    private List<String> updateImage(UpdateBoastCatPostRequest request, BoastCatPost post){
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

    // ========== 상세조회 + 조회수 통합 API (v1~v4) ==========

    // v1: 상세조회 + 더티 체킹 (Lost Update 발생)
    @Transactional
    public GetBoastCatPostResponse getBoastCatPostV1(Long id) {
        BoastCatPost post = boastCatPostRepository.findByIdWithUser(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        post.incrementView();
        return GetBoastCatPostResponse.from(post);
    }

    // v2: 상세조회 + 원자적 UPDATE (동시성 보장)
    @Transactional
    public GetBoastCatPostResponse getBoastCatPostV2(Long id) {
        BoastCatPost post = boastCatPostRepository.findByIdWithUser(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        boastCatPostRepository.incrementViewCount(id);
        return GetBoastCatPostResponse.from(post);
    }

    // v3: 상세조회 + Redis INCR (트랜잭션 없음 — Redis는 2PC 미지원)
    public GetBoastCatPostResponse getBoastCatPostV3(Long id, String clientIp) {
        BoastCatPost post = boastCatPostRepository.findByIdWithUser(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        viewCountService.incrementViewCount(PostType.BOAST, id, "ip:" + clientIp);
        return GetBoastCatPostResponse.from(post);
    }

    // v4: 상세조회 + 비관적 락 (SELECT FOR UPDATE, 처리량 낮음)
    @Transactional
    public GetBoastCatPostResponse getBoastCatPostV4(Long id) {
        BoastCatPost post = boastCatPostRepository.findByIdWithPessimisticLock(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        post.incrementView();
        return GetBoastCatPostResponse.from(post);
    }

}