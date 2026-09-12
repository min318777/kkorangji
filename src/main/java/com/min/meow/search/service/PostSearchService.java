package com.min.meow.search.service;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.search.dto.request.PostLikeSearchRequest;
import com.min.meow.search.dto.request.PostSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchService {

    // ngram_token_size(2) 미만 토큰은 FTS 인덱스에 없어 매치 불가
    private static final int MIN_TOKEN_LENGTH = 2;

    private final BoastCatPostRepository boastCatPostRepository;
    private final LostCatRepository lostCatRepository;

    // FTS 검색 (자랑글): 2글자 이상 토큰이 없으면 LIKE 자동 폴백
    public Page<BoastCatPostListResponse> searchByFts(PostSearchRequest request, Pageable pageable) {
        String keyword = request.getKeyword();

        if (requiresLikeFallback(keyword)) {
            log.debug("[자랑글 검색] FTS→LIKE 폴백 | keyword=\"{}\" | 이유=2글자 이상 토큰 없음", keyword);
            return boastCatPostRepository.search(keyword, keyword, request.getUserId(), pageable);
        }

        log.debug("[자랑글 검색] FTS | keyword=\"{}\"", keyword);
        return boastCatPostRepository.searchByKeyword(keyword, request.getUserId(), pageable);
    }

    // FTS 검색 (자랑글, 자연어 모드): LIKE 폴백 없이 항상 NATURAL LANGUAGE MODE로 검색 (50% 규칙 검증용)
    public Page<BoastCatPostListResponse> searchByNaturalLanguage(PostSearchRequest request, Pageable pageable) {
        String keyword = request.getKeyword();
        log.debug("[자랑글 검색] FTS(자연어 모드) | keyword=\"{}\"", keyword);
        return boastCatPostRepository.searchByNaturalLanguage(keyword, request.getUserId(), pageable);
    }

    // LIKE 검색 (자랑글): '%keyword%' 방식 (성능 비교용)
    public Page<BoastCatPostListResponse> searchByLike(PostLikeSearchRequest request, Pageable pageable) {
        String keyword = request.getTitle() != null ? request.getTitle() : request.getContents();
        if (keyword == null || keyword.length() < 2) {
            throw new CustomException(ErrorCode.SEARCH_KEYWORD_TOO_SHORT);
        }
        log.debug("[자랑글 검색] LIKE | keyword=\"{}\"", keyword);
        return boastCatPostRepository.search(
                request.getTitle(),
                request.getContents(),
                request.getUserId(),
                pageable
        );
    }

    // FTS 검색 (실종글): 2글자 이상 토큰이 없으면 LIKE 자동 폴백
    public Page<LostCatPostListResponse> searchLostByFts(PostSearchRequest request, Pageable pageable) {
        String keyword = request.getKeyword();

        if (requiresLikeFallback(keyword)) {
            log.debug("[실종글 검색] FTS→LIKE 폴백 | keyword=\"{}\" | 이유=2글자 이상 토큰 없음", keyword);
            return lostCatRepository.search(keyword, keyword, request.getUserId(), pageable);
        }

        log.debug("[실종글 검색] FTS | keyword=\"{}\"", keyword);
        return lostCatRepository.searchByKeyword(keyword, request.getUserId(), pageable);
    }

    // LIKE 검색 (실종글): '%keyword%' 방식 (성능 비교용)
    public Page<LostCatPostListResponse> searchLostByLike(PostLikeSearchRequest request, Pageable pageable) {
        String keyword = request.getTitle() != null ? request.getTitle() : request.getContents();
        if (keyword == null || keyword.length() < 2) {
            throw new CustomException(ErrorCode.SEARCH_KEYWORD_TOO_SHORT);
        }
        log.debug("[실종글 검색] LIKE | keyword=\"{}\"", keyword);
        return lostCatRepository.search(
                request.getTitle(),
                request.getContents(),
                request.getUserId(),
                pageable
        );
    }

    // 2글자 이상 토큰이 하나도 없으면 FTS로 검색할 대상 자체가 없어 LIKE로 폴백
    private boolean requiresLikeFallback(String keyword) {
        return Arrays.stream(keyword.trim().split("\\s+"))
                .noneMatch(token -> token.length() >= MIN_TOKEN_LENGTH);
    }
}
