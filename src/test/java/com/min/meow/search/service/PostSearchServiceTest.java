package com.min.meow.search.service;

import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.search.dto.request.PostLikeSearchRequest;
import com.min.meow.search.dto.request.PostSearchRequest;
import com.min.meow.search.repository.BoastCatPostSearchRepository;
import com.min.meow.search.repository.LostCatPostSearchRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostSearchService 유닛 테스트")
class PostSearchServiceTest {

    @InjectMocks
    private PostSearchService postSearchService;

    @Mock
    private BoastCatPostSearchRepository boastCatPostRepository;

    @Mock
    private LostCatPostSearchRepository lostCatRepository;

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    @DisplayName("2글자 이상 토큰이 있으면 FTS로 검색한다")
    void 검색어가_2글자_이상이면_FTS_검색() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("고양이").build();
        given(boastCatPostRepository.searchByKeyword("고양이", pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchByFts(request, pageable);

        // then
        then(boastCatPostRepository).should().searchByKeyword("고양이", pageable);
        then(boastCatPostRepository).should(never()).findByTitleContainingOrContentsContainingOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    @DisplayName("영문 등 한글이 아니어도 2글자 이상이면 FTS로 검색한다")
    void 한글이_아니어도_2글자_이상이면_FTS_검색() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("cat").build();
        given(boastCatPostRepository.searchByKeyword("cat", pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchByFts(request, pageable);

        // then
        then(boastCatPostRepository).should().searchByKeyword("cat", pageable);
    }

    @Test
    @DisplayName("2글자 이상 토큰이 하나라도 있으면 1글자 토큰이 섞여 있어도 FTS로 검색한다")
    void 짧은_토큰이_섞여도_2글자_이상_토큰이_있으면_FTS_검색() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("a 고양이").build();
        given(boastCatPostRepository.searchByKeyword("a 고양이", pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchByFts(request, pageable);

        // then
        then(boastCatPostRepository).should().searchByKeyword("a 고양이", pageable);
        then(boastCatPostRepository).should(never()).findByTitleContainingOrContentsContainingOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    @DisplayName("모든 토큰이 1글자면 LIKE로 폴백한다")
    void 모든_토큰이_1글자면_LIKE_폴백() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("a b").build();
        given(boastCatPostRepository.findByTitleContainingOrContentsContainingOrderByCreatedAtDesc("a b", "a b", pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchByFts(request, pageable);

        // then
        then(boastCatPostRepository).should().findByTitleContainingOrContentsContainingOrderByCreatedAtDesc("a b", "a b", pageable);
        then(boastCatPostRepository).should(never()).searchByKeyword(any(), any());
    }

    @Test
    @DisplayName("자연어 모드 검색은 짧은 토큰이 있어도 LIKE로 폴백하지 않고 항상 자연어 모드로 검색한다")
    void 자연어_모드_검색은_폴백_없이_항상_자연어_모드로_검색() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("고양이").build();
        given(boastCatPostRepository.searchByNaturalLanguage("고양이", pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchByNaturalLanguage(request, pageable);

        // then
        then(boastCatPostRepository).should().searchByNaturalLanguage("고양이", pageable);
        then(boastCatPostRepository).should(never()).findByTitleContainingOrContentsContainingOrderByCreatedAtDesc(any(), any(), any());
        then(boastCatPostRepository).should(never()).searchByKeyword(any(), any());
    }

    @Test
    @DisplayName("실종글 검색도 동일한 기준으로 FTS/LIKE를 분기한다")
    void 실종글_검색도_동일한_기준으로_분기() {
        // given
        PostSearchRequest request = PostSearchRequest.builder().keyword("나비").build();
        given(lostCatRepository.searchByKeyword("나비", pageable))
                .willReturn(new PageImpl<>(java.util.List.of()));

        // when
        postSearchService.searchLostByFts(request, pageable);

        // then
        then(lostCatRepository).should().searchByKeyword("나비", pageable);
    }

    @Test
    @DisplayName("LIKE 검색은 keyword로 제목/내용을 함께 조회하도록 위임한다")
    void LIKE_검색은_keyword로_위임() {
        // given
        PostLikeSearchRequest request = PostLikeSearchRequest.builder().keyword("고양이").build();
        BoastCatPost post = BoastCatPost.builder().title("우리 고양이").contents("귀여워요").build();
        given(boastCatPostRepository.findByTitleContainingOrContentsContainingOrderByCreatedAtDesc("고양이", "고양이", pageable))
                .willReturn(new PageImpl<>(java.util.List.of(post)));

        // when
        postSearchService.searchByLike(request, pageable);

        // then
        then(boastCatPostRepository).should().findByTitleContainingOrContentsContainingOrderByCreatedAtDesc("고양이", "고양이", pageable);
    }

    @Test
    @DisplayName("실종글 LIKE 검색도 keyword로 제목/내용을 함께 조회하도록 위임한다")
    void 실종글_LIKE_검색은_keyword로_위임() {
        // given
        PostLikeSearchRequest request = PostLikeSearchRequest.builder().keyword("나비").build();
        LostCatPost post = LostCatPost.builder().title("나비를 찾아요").contents("실종되었습니다").build();
        given(lostCatRepository.findByTitleContainingOrContentsContainingOrderByCreatedAtDesc("나비", "나비", pageable))
                .willReturn(new PageImpl<>(java.util.List.of(post)));

        // when
        postSearchService.searchLostByLike(request, pageable);

        // then
        then(lostCatRepository).should().findByTitleContainingOrContentsContainingOrderByCreatedAtDesc("나비", "나비", pageable);
    }

}
