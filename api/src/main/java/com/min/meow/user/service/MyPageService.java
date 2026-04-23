package com.min.meow.user.service;

import com.min.meow.global.PostType;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.comment.entity.Comment;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.user.dto.reponse.*;
import com.min.meow.user.dto.request.UpdateProfileRequest;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마이페이지 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final BoastCatPostRepository boastCatPostRepository;
    private final LostCatRepository lostCatRepository;
    private final CommentRepository commentRepository;

    public MyPageSummaryResponse getMyPageSummary(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNREGISTERED_USER));

        // 캐싱된 통계 메서드 호출 (캐시 히트 시 COUNT 쿼리 3개 생략)
        long[] stats = getMyPageStats(userId);

        return MyPageSummaryResponse.from(user, stats[0], stats[1], stats[2]);
    }

    /**
     * 마이페이지 통계 조회 (캐싱 적용)
     *
     * 캐시 설정:
     * - 캐시명: user:stats
     * - 키: userId (예: user:stats::1)
     * - TTL: 10분
     *
     * 무효화 트리거:
     * - 자랑글 작성/삭제 → BoastCatPostService
     * - 실종글 작성/삭제 → LostCatPostService
     * - 댓글 작성/삭제   → CommentService
     *
     * @return [자랑글 수, 실종글 수, 댓글 수]
     */
    @Cacheable(cacheNames = "user:stats", key = "#userId")
    public long[] getMyPageStats(Long userId) {
        // 캐시 미스 시에만 COUNT 쿼리 3개 실행
        long boastCatPostCount = userRepository.countBoastCatPostsByUserId(userId);
        long lostCatPostCount = userRepository.countLostCatPostsByUserId(userId);
        long commentCount = commentRepository.countByUserId(userId);
        return new long[]{boastCatPostCount, lostCatPostCount, commentCount};
    }

    /**
     * 내가 쓴 게시글 목록 조회
     * - BOAST: 자랑글만, LOST: 실종글만 조회 (ALL 타입 미지원 — OOM 위험)
     */
    public MyPostListResponse getMyPosts(Long userId, Pageable pageable, PostType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNREGISTERED_USER));

        // 타입에 따라 게시글 조회 (DB 레벨 페이징)
        Page<MyPostDto> postPage;
        if (type == PostType.BOAST) {
            // 자랑글만 조회
            postPage = getBoastPostsOnly(user.getId(), pageable);
        } else if (type == PostType.LOST) {
            // 실종글만 조회
            postPage = getLostPostsOnly(user.getId(), pageable);
        } else {
            // ALL 타입: 전체 메모리 로딩 후 수동 페이징 → OOM 위험으로 미지원
            throw new CustomException(ErrorCode.INVALID_POST_TYPE);
        }

        return MyPostListResponse.from(postPage);
    }

    /**
     * 내가 쓴 댓글 목록 조회
     */
    public MyCommentListResponse getMyComments(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNREGISTERED_USER));

        // 2. 댓글 조회 (페이징)
        Page<Comment> commentPage = commentRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        // 3. DTO 변환
        Page<MyCommentDto> commentDtoPage = commentPage.map(MyCommentDto::from);

        // 4. Response 생성
        return MyCommentListResponse.from(commentDtoPage);
    }

    /**
     * 프로필 수정 — 현재는 닉네임만 변경 가능
     */
    @Transactional
    public MyPageSummaryResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 도메인 메서드로 닉네임 변경 (setter 대신)
        user.updateNickname(request.getNickname());

        // 캐싱된 통계 메서드 재사용 (통계는 닉네임 변경과 무관하므로 캐시 유지)
        long[] stats = getMyPageStats(userId);

        return MyPageSummaryResponse.from(user, stats[0], stats[1], stats[2]);
    }

    private Page<MyPostDto> getBoastPostsOnly(Long userId, Pageable pageable) {
        Page<BoastCatPost> boastPosts = boastCatPostRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return boastPosts.map(MyPostDto::from);
    }

    private Page<MyPostDto> getLostPostsOnly(Long userId, Pageable pageable) {
        Page<LostCatPost> lostPosts = lostCatRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return lostPosts.map(MyPostDto::fromLostCatPost);
    }
}
