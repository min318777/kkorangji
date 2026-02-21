package com.min.meow.user.service.impl;

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
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 마이페이지 서비스 구현 클래스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageServiceImpl implements MyPageService {

    private final UserRepository userRepository;
    private final BoastCatPostRepository boastCatPostRepository;
    private final LostCatRepository lostCatRepository;
    private final CommentRepository commentRepository;

    @Override
    public MyPageSummaryResponse getMyPageSummary(String loginId) {

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNREGISTERED_USER));

        long boastCatPostCount = userRepository.countBoastCatPostsByUserId(user.getId());
        long lostCatPostCount = userRepository.countLostCatPostsByUserId(user.getId());
        long commentCount = commentRepository.countByUser(user);


        return MyPageSummaryResponse.from(user, boastCatPostCount, lostCatPostCount, commentCount);
    }

    /**
     * 내가 쓴 게시글 목록 조회
     * - type에 따라 전체, 자랑글만, 실종글만 조회 가능
     */
    @Override
    public MyPostListResponse getMyPosts(String loginId, Pageable pageable, PostType type) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNREGISTERED_USER));

        // 2. 타입에 따라 게시글 조회
        Page<MyPostDto> postPage;
        if (type == PostType.BOAST) {
            // 자랑글만 조회
            postPage = getBoastPostsOnly(user.getId(), pageable);
        } else if (type == PostType.LOST) {
            // 실종글만 조회
            postPage = getLostPostsOnly(user.getId(), pageable);
        } else {
            postPage = getAllPosts(user.getId(), pageable);
        }

        // 3. Response 생성
        return MyPostListResponse.from(postPage);
    }

    /**
     * 내가 쓴 댓글 목록 조회
     * - 댓글이 달린 게시글 정보 포함
     * - 최신순 정렬
     * - 페이징 처리
     */
    @Override
    public MyCommentListResponse getMyComments(Long userId, Pageable pageable) {
        // 1. 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNREGISTERED_USER));

        // 2. 댓글 조회 (페이징)
        Page<Comment> commentPage = commentRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        // 3. DTO 변환
        Page<MyCommentDto> commentDtoPage = commentPage.map(MyCommentDto::from);

        // 4. Response 생성
        return MyCommentListResponse.from(commentDtoPage);
    }

    private Page<MyPostDto> getBoastPostsOnly(Long userId, Pageable pageable) {
        Page<BoastCatPost> boastPosts = boastCatPostRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return boastPosts.map(MyPostDto::from);
    }

    private Page<MyPostDto> getLostPostsOnly(Long userId, Pageable pageable) {
        Page<LostCatPost> lostPosts = lostCatRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return lostPosts.map(MyPostDto::fromLostCatPost);
    }

    private Page<MyPostDto> getAllPosts(Long userId, Pageable pageable) {
        // 1. 각각의 게시글 리스트를 모두 가져옴 (페이징 전)
        List<BoastCatPost> boastPosts = boastCatPostRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .getContent();

        List<LostCatPost> lostPosts = lostCatRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .getContent();

        // 2. DTO로 변환
        List<MyPostDto> allPosts = new ArrayList<>();
        allPosts.addAll(boastPosts.stream()
                .map(MyPostDto::from)
                .collect(Collectors.toList()));
        allPosts.addAll(lostPosts.stream()
                .map(MyPostDto::fromLostCatPost)
                .collect(Collectors.toList()));

        allPosts.sort(Comparator.comparing(MyPostDto::getCreatedAt).reversed());

        // 4. 수동 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allPosts.size());

        List<MyPostDto> pagedPosts = allPosts.subList(start, end);

        return new PageImpl<>(pagedPosts, pageable, allPosts.size());
    }
}    