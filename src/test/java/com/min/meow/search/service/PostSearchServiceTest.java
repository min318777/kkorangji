package com.min.meow.search.service;

import com.min.meow.post.dto.response.BoastCatPostListResponse;
import com.min.meow.post.dto.response.LostCatPostListResponse;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.search.dto.request.PostSearchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostSearchService 유닛 테스트")
class PostSearchServiceTest {

    @InjectMocks
    private PostSearchService postSearchService;

    @Mock
    private BoastCatPostRepository boastCatPostRepository;

    @Mock
    private LostCatRepository lostCatRepository;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    @DisplayName("2글자 이상 토큰이 있으면 FTS로 검색한다")
    void 검색어가_2글자_이상이면_FTS_검색() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("고양이").build();
        given(boastCatPostRepository.searchByKeyword("고양이", null, pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchByFts(request, pageable);

        // then
        then(boastCatPostRepository).should().searchByKeyword("고양이", null, pageable);
        then(boastCatPostRepository).should(never()).search(any(), any(), any(), any());
    }

    @Test
    @DisplayName("영문 등 한글이 아니어도 2글자 이상이면 FTS로 검색한다")
    void 한글이_아니어도_2글자_이상이면_FTS_검색() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("cat").build();
        given(boastCatPostRepository.searchByKeyword("cat", null, pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchByFts(request, pageable);

        // then
        then(boastCatPostRepository).should().searchByKeyword("cat", null, pageable);
    }

    @Test
    @DisplayName("2글자 이상 토큰이 하나라도 있으면 1글자 토큰이 섞여 있어도 FTS로 검색한다")
    void 짧은_토큰이_섞여도_2글자_이상_토큰이_있으면_FTS_검색() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("a 고양이").build();
        given(boastCatPostRepository.searchByKeyword("a 고양이", null, pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchByFts(request, pageable);

        // then
        then(boastCatPostRepository).should().searchByKeyword("a 고양이", null, pageable);
        then(boastCatPostRepository).should(never()).search(any(), any(), any(), any());
    }

    @Test
    @DisplayName("모든 토큰이 1글자면 LIKE로 폴백한다")
    void 모든_토큰이_1글자면_LIKE_폴백() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("a b").build();
        given(boastCatPostRepository.search("a b", "a b", null, pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchByFts(request, pageable);

        // then
        then(boastCatPostRepository).should().search("a b", "a b", null, pageable);
        then(boastCatPostRepository).should(never()).searchByKeyword(any(), any(), any());
    }

    @Test
    @DisplayName("실종글 검색도 동일한 기준으로 FTS/LIKE를 분기한다")
    void 실종글_검색도_동일한_기준으로_분기() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("나비").build();
        given(lostCatRepository.searchByKeyword("나비", null, pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchLostByFts(request, pageable);

        // then
        then(lostCatRepository).should().searchByKeyword("나비", null, pageable);
    }
}
