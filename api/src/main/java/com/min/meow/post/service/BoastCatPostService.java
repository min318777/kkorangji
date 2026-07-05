package com.min.meow.post.service;


import com.min.meow.config.S3Service;
import com.min.meow.common.PageResponse;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.common.SecurityUtil;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final BoastCatPostCountCacheService countCacheService;

    // ========== 조회 ==========

    /**
     * 모든 글 조회 (Projection 적용으로 성능 최적화)
     * 성능 개선 내역:
     * - Before: Entity 전체 조회 + DTO 변환 → contents, imageUrls, comments 모두 조회
     * - After: Projection으로 필요한 7개 컬럼만 SELECT (id, title, writer, likeCount, commentCount, view, createdAt)
     */
    public PageResponse<BoastCatPostListResponse> getAllBoastCatPosts(Pageable pageable){
        // COUNT는 캐시에서, content는 DB에서 조회 (COUNT 쿼리 성능 개선)
        long total = countCacheService.countAll();
        Page<BoastCatPostListResponse> posts = new PageImpl<>(
                boastCatPostRepository.findContentWithCoveringIndex(pageable),
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
     * - 새 이미지: newImageKeys의 S3 key를 CloudFront URL로 변환
     * - 유지할 이미지: keepImageUrls 그대로 사용
     * - 삭제할 이미지: deleteImageUrls의 URL에서 S3 key 추출 후 삭제
     */
    @Transactional
    public UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest, Long boastCatPostId, Long userId){

        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 수정은 본인만 허용 (관리자도 타인 글 수정 불가)
        if (!boastCatPost.isAuthor(writer)) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        List<String> finalImageUrls = updateImage(updateBoastCatPostRequest);
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
    public void deleteBoastCatPost(Long boastCatPostId, Long userId){
        countCacheService.evict();
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        // 본인이 아니고 관리자 권한(post:delete)도 없으면 → 403
        if (!boastCatPost.isAuthor(writer)
                && !SecurityUtil.hasAuthority("post:delete")) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        // S3 이미지 삭제 (DB 삭제 전에 URL 추출)
        List<String> keys = boastCatPost.getImageUrls().stream()
                .map(s3Service::extractKeyFromUrl)
                .filter(key -> key != null && !key.isEmpty())
                .toList();
        boastCatPostRepository.deleteById(boastCatPostId);
        s3Service.deleteFiles(keys);
        log.info("게시글 삭제 완료 - userId: {}, postId: {}", userId, boastCatPostId);
    }

    /**
     * 이미지 업데이트 처리 (Presigned URL 방식)
     * @param request 수정 요청 DTO
     * @return 최종 이미지 URL 목록 (CloudFront URL)
     */
    private List<String> updateImage(UpdateBoastCatPostRequest request){
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

    // ========== 조회수 ==========

    /**
     * 조회수 증가 - 더티 체킹 방식 (v1 - 동시성 이슈 있음)
     * 동시성 문제 (Lost Update):
     * 이 방식은 아래와 같은 Read-Modify-Write 패턴으로 동작합니다:
     * 1. SELECT * FROM boast_cat_post WHERE id = ? (조회)
     * 2. Java에서 view++ 연산 수행
     * 3. UPDATE boast_cat_post SET view = 101 WHERE id = ? (절대값으로 UPDATE)
     * 문제 시나리오 (현재 view = 100, 동시 2개 요청):
     * - Thread A: view 읽기 (100) → view++ → 101로 UPDATE
     * - Thread B: view 읽기 (100) → view++ → 101로 UPDATE (동시에!)
     * - 결과: 2번 증가 요청 → 실제 1만 증가 (Lost Update)
     */
    @Deprecated
    @Transactional
    public void incrementViewCountWithDirtyChecking(Long boastCatPostId) {
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 더티 체킹으로 조회수 증가 (동시성 이슈 발생 가능)
        boastCatPost.incrementView();
        // 트랜잭션 종료 시 JPA가 변경 감지하여 UPDATE 쿼리 실행
    }

    /**
     * 조회수 증가 - 원자적 쿼리 방식 (v2 - 개선된 버전)
     * DB 레벨에서 view = view + 1을 수행하여 Race Condition을 방지합니다.
     * 여러 스레드가 동시에 호출해도 정확한 조회수가 보장됩니다.
     * 실행되는 쿼리:
     * UPDATE boast_cat_post SET view = view + 1 WHERE id = ?
     * 이 쿼리는 DB 레벨에서 원자적으로 실행되므로:
     * - Read-Modify-Write 패턴이 아님
     * - 동시 요청 시에도 모든 증가가 정확히 반영됨
     */
    @Transactional
    public void incrementViewCount(Long boastCatPostId) {
        int updatedCount = boastCatPostRepository.incrementViewCount(boastCatPostId);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
    }

    /**
     * 조회수 증가 - 비관적 락 방식 (v4)
     * 실행 순서:
     * 1. SELECT ... FOR UPDATE → 행 X-Lock 획득 (다른 트랜잭션 접근 차단)
     * 2. Java에서 view + 1 (더티 체킹)
     * 3. 트랜잭션 종료 → UPDATE SET view = ? → 락 해제
     * v1(더티 체킹)과 다른 점:
     * - v1은 락 없이 SELECT → 동시 요청이 같은 값을 읽어 Lost Update 발생
     * - v4는 SELECT FOR UPDATE → 순서 강제, 항상 최신값 읽음 → 정합성 보장
     * 단점: 락 유지 시간 = SELECT ~ 커밋 (v2 원자적 UPDATE보다 훨씬 길다)
     * → 대용량 트래픽 시 락 대기 폭증 → v2, v3보다 처리량 낮음
     */
    @Transactional
    public void incrementViewCountWithPessimisticLock(Long boastCatPostId) {
        // SELECT FOR UPDATE → 행 잠금 (다른 트랜잭션은 이 커밋 전까지 대기)
        BoastCatPost post = boastCatPostRepository.findByIdWithPessimisticLock(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 잠금 상태에서 안전하게 최신값 +1
        post.incrementView();
        // 트랜잭션 종료 → UPDATE SET view = {최신값+1} → 락 해제
    }

}