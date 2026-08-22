package com.min.meow.search.service;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.repository.BoastCatPostRepositoryImpl;
import com.min.meow.post.repository.LostCatRepositoryImpl;
import com.min.meow.search.dto.request.PostLikeSearchRequest;
import com.min.meow.search.dto.request.PostSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchService {

    private final BoastCatPostRepositoryImpl boastCatPostRepositoryImpl;
    private final LostCatRepositoryImpl lostCatRepositoryImpl;

    // FTS 검색 (자랑글): 한글 완성형 없으면 LIKE 자동 폴백
    public Page<BoastCatPostListResponse> searchByFts(PostSearchRequest request, Pageable pageable) {
        String keyword = request.getKeyword();

        if (keyword == null || keyword.length() < 2) {
            throw new CustomException(ErrorCode.SEARCH_KEYWORD_TOO_SHORT);
        }

        if (requiresLikeFallback(keyword)) {
            log.info("[자랑글 검색] FTS→LIKE 폴백 | keyword=\"{}\" | 이유=한글 완성형 없음", keyword);
            return boastCatPostRepositoryImpl.search(keyword, keyword, request.getUserId(), pageable);
        }

        log.info("[자랑글 검색] FTS | keyword=\"{}\"", keyword);
        return boastCatPostRepositoryImpl.searchByKeyword(keyword, request.getUserId(), pageable);
    }

    // LIKE 검색 (자랑글): '%keyword%' 방식 (성능 비교용)
    public Page<BoastCatPostListResponse> searchByLike(PostLikeSearchRequest request, Pageable pageable) {
        String keyword = request.getTitle() != null ? request.getTitle() : request.getContents();
        if (keyword == null || keyword.length() < 2) {
            throw new CustomException(ErrorCode.SEARCH_KEYWORD_TOO_SHORT);
        }
        log.info("[자랑글 검색] LIKE | keyword=\"{}\"", keyword);
        return boastCatPostRepositoryImpl.search(
                request.getTitle(),
                request.getContents(),
                request.getUserId(),
                pageable
        );
    }

    // FTS 검색 (실종글): 한글 완성형 없으면 LIKE 자동 폴백
    public Page<LostCatPostListResponse> searchLostByFts(PostSearchRequest request, Pageable pageable) {
        String keyword = request.getKeyword();

        if (keyword == null || keyword.length() < 2) {
            throw new CustomException(ErrorCode.SEARCH_KEYWORD_TOO_SHORT);
        }

        if (requiresLikeFallback(keyword)) {
            log.info("[실종글 검색] FTS→LIKE 폴백 | keyword=\"{}\" | 이유=한글 완성형 없음", keyword);
            return lostCatRepositoryImpl.search(keyword, keyword, request.getUserId(), pageable);
        }

        log.info("[실종글 검색] FTS | keyword=\"{}\"", keyword);
        return lostCatRepositoryImpl.searchByKeyword(keyword, request.getUserId(), pageable);
    }

    // LIKE 검색 (실종글): '%keyword%' 방식 (성능 비교용)
    public Page<LostCatPostListResponse> searchLostByLike(PostLikeSearchRequest request, Pageable pageable) {
        String keyword = request.getTitle() != null ? request.getTitle() : request.getContents();
        if (keyword == null || keyword.length() < 2) {
            throw new CustomException(ErrorCode.SEARCH_KEYWORD_TOO_SHORT);
        }
        log.info("[실종글 검색] LIKE | keyword=\"{}\"", keyword);
        return lostCatRepositoryImpl.search(
                request.getTitle(),
                request.getContents(),
                request.getUserId(),
                pageable
        );
    }

    // 한글 완성형이 하나도 없으면 ngram FTS 토큰 생성 불가
    private boolean requiresLikeFallback(String keyword) {
        return !keyword.matches(".*[가-힣].*");
    }
}
