package com.min.meow.post.service.impl;


import com.min.meow.config.S3Service;
import com.min.meow.global.PageResponse;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.service.BoastCatPostService;
import com.min.meow.global.SecurityUtil;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoastCatPostServiceImpl implements BoastCatPostService {
    private final BoastCatPostRepository boastCatPostRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    /**
     * 모든 글 조회 (Projection 적용으로 성능 최적화)
     * 성능 개선 내역:
     * - Before: Entity 전체 조회 + DTO 변환 → contents, imageUrls, comments 모두 조회
     * - After: Projection으로 필요한 7개 컬럼만 SELECT (id, title, writer, likeCount, commentCount, view, createdAt)
     * 실행되는 쿼리:
     * SELECT b.id, b.title, u.login_id, b.like_count, b.comment_count, b.view, b.created_at
     * FROM boast_cat_post b LEFT JOIN user u ON b.user_id = u.id
     * ORDER BY b.created_at DESC LIMIT ? OFFSET ?
     */
    @Override
    public PageResponse<BoastCatPostListResponse> getAllBoastCatPosts(Pageable pageable){
        // Projection으로 DB에서 필요한 컬럼만 조회 (Entity 변환 불필요)
        Page<BoastCatPostListResponse> posts = boastCatPostRepository.findAllWithProjection(pageable);

        return PageResponse.from(posts);
    }

    /**
     * 글 상세 조회 (N+1 최적화 적용 + 캐싱)
     * findByIdWithUser()로 User를 Fetch Join하여 N+1 문제 해결
     * - User: Fetch Join (N:1 관계 → 카테시안 곱 없음)
     * - imageUrls: @BatchSize(100) 적용 (1:N 관계)
     * - comments: @BatchSize(100) 적용 (1:N 관계)
     * 조회수 증가는 별도 API로 분리됨
     *
     * 캐시 설정:
     * - 캐시명: post:boast:detail
     * - 키: 게시글 ID (예: post:boast:detail::123)
     * - TTL: 10분
     */
    @Override
    @Cacheable(cacheNames = "post:boast:detail", key = "#boastCatPostId")
    public GetBoastCatPostResponse getBoastCatPost(Long boastCatPostId){
        BoastCatPost boastCatPost = boastCatPostRepository.findByIdWithUser(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        return GetBoastCatPostResponse.toResponse(boastCatPost);
    }

    /**
     * 조회수 증가 - 원자적 쿼리 방식 (v2 - 개선된 버전)
     *
     * DB 레벨에서 view = view + 1을 수행하여 Race Condition을 방지합니다.
     * 여러 스레드가 동시에 호출해도 정확한 조회수가 보장됩니다.
     *
     * 실행되는 쿼리:
     * UPDATE boast_cat_post SET view = view + 1 WHERE id = ?
     *
     * 이 쿼리는 DB 레벨에서 원자적으로 실행되므로:
     * - Read-Modify-Write 패턴이 아님
     * - 동시 요청 시에도 모든 증가가 정확히 반영됨
     *
     * K6 동시성 테스트 결과:
     * - 더티 체킹 방식: 1000 VU 동시 요청 → 약 800~900 증가 (Lost Update)
     * - 원자적 쿼리: 1000 VU 동시 요청 → 정확히 1000 증가 ✅
     */
    @Override
    @Transactional
    public void incrementViewCount(Long boastCatPostId) {
        int updatedCount = boastCatPostRepository.incrementViewCount(boastCatPostId);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
    }

    /**
     * 조회수 증가 - 더티 체킹 방식 (v1 - 동시성 이슈 있음)
     *
     * ⚠️ 동시성 문제 (Lost Update):
     * 이 방식은 아래와 같은 Read-Modify-Write 패턴으로 동작합니다:
     * 1. SELECT * FROM boast_cat_post WHERE id = ? (조회)
     * 2. Java에서 view++ 연산 수행
     * 3. UPDATE boast_cat_post SET view = 101 WHERE id = ? (절대값으로 UPDATE)
     *
     * 문제 시나리오 (현재 view = 100, 동시 2개 요청):
     * - Thread A: view 읽기 (100) → view++ → 101로 UPDATE
     * - Thread B: view 읽기 (100) → view++ → 101로 UPDATE (동시에!)
     * - 결과: 2번 증가 요청 → 실제 1만 증가 (Lost Update)
     *
     * K6 동시성 테스트로 이 문제를 발견하여
     * incrementViewCount() 원자적 쿼리 방식으로 개선하였습니다.
     *
     * @deprecated 동시성 이슈로 인해 incrementViewCount() 사용 권장
     */
    @Override
    @Transactional
    public void incrementViewCountWithDirtyChecking(Long boastCatPostId) {
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 더티 체킹으로 조회수 증가 (동시성 이슈 발생 가능)
        boastCatPost.incrementView();
        // 트랜잭션 종료 시 JPA가 변경 감지하여 UPDATE 쿼리 실행
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
     * 캐시 무효화:
     * - post:boast:recent (새 글이 메인페이지 최근글 목록에 반영되어야 함)
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "post:boast:recent", allEntries = true)
    public CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, String loginId){

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // S3 key를 CloudFront URL로 변환
        List<String> imageUrls = new ArrayList<>();
        if (createBoastCatPostRequest.getImageKeys() != null && !createBoastCatPostRequest.getImageKeys().isEmpty()) {
            imageUrls = s3Service.toCloudFrontUrls(createBoastCatPostRequest.getImageKeys());
        }

        // 엔티티 생성
        BoastCatPost boastCatPost = BoastCatPost.builder()
                .title(createBoastCatPostRequest.getTitle())
                .contents(createBoastCatPostRequest.getContent())
                .imageUrls(imageUrls)
                .user(writer)
                .build();
        boastCatPostRepository.save(boastCatPost);

        return CreateBoastCatPostResponse.from(boastCatPost);
    }

    /**
     * 글 수정 (Presigned URL 기반 이미지 업로드)
     *
     * 이미지 처리:
     * - 새 이미지: newImageKeys의 S3 key를 CloudFront URL로 변환
     * - 유지할 이미지: keepImageUrls 그대로 사용
     * - 삭제할 이미지: deleteImageUrls의 URL에서 S3 key 추출 후 삭제
     *
     * 캐시 무효화:
     * - post:boast:recent (수정된 내용이 메인페이지 최근글 목록에 반영되어야 함)
     * - post:boast:detail (해당 게시글 상세 캐시 무효화)
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "post:boast:recent", allEntries = true),
            @CacheEvict(cacheNames = "post:boast:detail", key = "#boastCatPostId")
    })
    public UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest, Long boastCatPostId, String loginId){

        User writer = userRepository.findByLoginId(loginId)
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
     * 캐시 무효화:
     * - post:boast:recent (삭제된 글이 메인페이지 최근글 목록에서 제거되어야 함)
     * - post:boast:detail (해당 게시글 상세 캐시 무효화)
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "post:boast:recent", allEntries = true),
            @CacheEvict(cacheNames = "post:boast:detail", key = "#boastCatPostId")
    })
    public void deleteBoastCatPost(Long boastCatPostId, String loginId){
        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        // 본인이 아니고 관리자 권한(post:delete)도 없으면 → 403
        if (!boastCatPost.isAuthor(writer)
                && !SecurityUtil.hasAuthority("post:delete")) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
        boastCatPostRepository.deleteById(boastCatPostId);
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


    /**
     * 최근 게시물 20개 조회 (DTO Projection + 캐싱 적용)
     *
     * 성능 최적화:
     * - QueryDSL Projection으로 DB에서 필요한 컬럼만 SELECT
     * - Entity 변환 오버헤드 제거 (직접 DTO로 매핑)
     * - contents, imageUrls 등 불필요한 데이터 조회 제거
     * - BoastCatPostListResponse를 재사용하여 코드 중복 제거
     *
     * 캐시 설정:
     * - 캐시명: post:boast:recent
     * - 키: 없음 (단일 목록이므로 캐시명 자체가 키)
     * - TTL: 5분
     */
    @Override
    @Cacheable(cacheNames = "post:boast:recent")
    public List<BoastCatPostListResponse> getRecentBoastCatPosts() {
        // DTO Projection으로 필요한 컬럼만 조회 (Entity 변환 없음)
        return boastCatPostRepository.findTop20RecentWithProjection();
    }
}
