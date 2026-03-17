package com.min.meow.post.service;

import com.min.meow.config.S3Service;
import com.min.meow.global.PageResponse;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.GetLostCatPostResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.global.SecurityUtil;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 실종 고양이 게시글 서비스
 *
 * Presigned URL 기반 이미지 업로드 방식 적용:
 * - 클라이언트가 S3에 직접 업로드 후 key 전달
 * - 서버는 key를 CloudFront URL로 변환하여 저장
 * - 서버 트래픽 비용 절감 및 보안 강화
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostCatPostService {

    private final LostCatRepository lostCatRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    /**
     * 모든 실종 고양이 게시글 목록 조회 (Projection 적용으로 성능 최적화)
     *
     * 성능 개선 내역:
     * - Before: Entity 전체 조회 + DTO 변환 → contents, imageUrls, comments 모두 조회
     *           → LazyInitializationException 발생 가능
     * - After: Projection으로 필요한 9개 컬럼만 SELECT
     *          (id, title, writer, catName, lostLocation, commentCount, view, isCompleted, createdAt)
     *
     * 실행되는 쿼리:
     * SELECT l.id, l.title, u.login_id, l.cat_name, l.lost_location,
     *        l.comment_count, l.view, l.is_completed, l.created_at
     * FROM lost_cat_post l
     * LEFT JOIN users u ON l.user_id = u.id
     * ORDER BY l.created_at DESC
     * LIMIT ? OFFSET ?
     */
    public PageResponse<LostCatPostListResponse> getAllLostCatPosts(Pageable pageable){
        // Projection으로 DB에서 필요한 컬럼만 조회 (Entity 변환 불필요)
        Page<LostCatPostListResponse> posts = lostCatRepository.findAllWithProjection(pageable);

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
     * - 캐시명: post:lost:detail
     * - 키: 게시글 ID (예: post:lost:detail::123)
     * - TTL: 10분
     */
    @Cacheable(cacheNames = "post:lost:detail", key = "#lostCatPostId")
    public GetLostCatPostResponse getLostCatPost(Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findByIdWithUser(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        return GetLostCatPostResponse.toResponse(lostCatPost);
    }


    /**
     * 조회수 증가 - 원자적 쿼리 방식 (v2 - 개선된 버전)
     *
     * DB 레벨에서 view = view + 1을 수행하여 Race Condition을 방지합니다.
     * 여러 스레드가 동시에 호출해도 정확한 조회수가 보장됩니다.
     *
     * 실행되는 쿼리:
     * UPDATE lost_cat_post SET view = view + 1 WHERE id = ?
     *
     * 이 쿼리는 DB 레벨에서 원자적으로 실행되므로:
     * - Read-Modify-Write 패턴이 아님
     * - 동시 요청 시에도 모든 증가가 정확히 반영됨
     *
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

    /**
     * 조회수 증가 - 더티 체킹 방식 (v1 - 동시성 이슈 있음)
     *
     * ⚠️ 동시성 문제 (Lost Update):
     * 이 방식은 아래와 같은 Read-Modify-Write 패턴으로 동작합니다:
     * 1. SELECT * FROM lost_cat_post WHERE id = ? (조회)
     * 2. Java에서 view++ 연산 수행
     * 3. UPDATE lost_cat_post SET view = 101 WHERE id = ? (절대값으로 UPDATE)
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
    @Deprecated
    @Transactional
    public void incrementViewCountWithDirtyChecking(Long lostCatPostId) {
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 더티 체킹으로 조회수 증가 (동시성 이슈 발생 가능)
        lostCatPost.incrementView();
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
     * - post:lost:recent (새 글이 메인페이지 최근글 목록에 반영되어야 함)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "post:lost:recent", allEntries = true),
            // 실종글 작성 시 마이페이지 통계(실종글 수) 캐시 무효화
            @CacheEvict(cacheNames = "user:stats", key = "#loginId")
    })
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
     * 캐시 무효화:
     * - post:lost:recent (수정된 내용이 메인페이지 최근글 목록에 반영되어야 함)
     * - post:lost:detail (해당 게시글 상세 캐시 무효화)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "post:lost:recent", allEntries = true),
            @CacheEvict(cacheNames = "post:lost:detail", key = "#lostCatPostId")
    })
    public UpdateLostCatPostResponse updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest, String loginId){
        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 수정은 본인만 허용 (관리자도 타인 글 수정 불가)
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

    /**
     * 글 삭제
     *
     * 캐시 무효화:
     * - post:lost:recent (삭제된 글이 메인페이지 최근글 목록에서 제거되어야 함)
     * - post:lost:detail (해당 게시글 상세 캐시 무효화)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "post:lost:recent", allEntries = true),
            @CacheEvict(cacheNames = "post:lost:detail", key = "#lostCatPostId"),
            // 실종글 삭제 시 마이페이지 통계(실종글 수) 캐시 무효화
            @CacheEvict(cacheNames = "user:stats", key = "#loginId")
    })
    public void deleteLostCatPost(Long lostCatPostId, String loginId) {

        User writer = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // 본인이 아니고 관리자 권한(post:delete)도 없으면 → 403
        if (!lostCatPost.isAuthor(writer)
                && !SecurityUtil.hasAuthority("post:delete")) {
            throw new CustomException(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        lostCatRepository.deleteById(lostCatPostId);
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

    /**
     * 최근 실종글 20개 조회 (DTO Projection + 캐싱 적용)
     *
     * 성능 최적화:
     * - QueryDSL Projection으로 DB에서 필요한 컬럼만 SELECT
     * - Entity 변환 오버헤드 제거 (직접 DTO로 매핑)
     * - contents, imageUrls 등 불필요한 데이터 조회 제거
     *
     * 캐시 설정:
     * - 캐시명: post:lost:recent
     * - 키: 없음 (단일 목록이므로 캐시명 자체가 키)
     * - TTL: 5분
     */
    @Cacheable(cacheNames = "post:lost:recent")
    public List<LostCatPostListResponse> getRecentLostCatPosts() {
        // DTO Projection으로 필요한 컬럼만 조회 (Entity 변환 없음)
        return lostCatRepository.findTop20RecentWithProjection();
    }
}
