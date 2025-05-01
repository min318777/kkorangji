package com.min.meow.lostcatpost.service;

import com.min.meow.lostcatpost.domain.dto.LostCatPostDto;
import com.min.meow.lostcatpost.entity.LostCatPost;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class LostCatPostServiceTest {

    @Mock
    private LostCatRepository lostCatRepository;

    @InjectMocks
    private LostCatPostService lostCatPostService;

    @Test
    @DisplayName("모든 고양이 찾기 글을 조회한다.")
    void getAllLostCatPost(){
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<LostCatPost> posts = List.of(LostCatPost.builder().title("고양이 공고1").build(),
                                            LostCatPost.builder().title("고양이 공고2").build());
        Page<LostCatPost> mockPage = new PageImpl<>(posts, pageable, posts.size());
        when(lostCatRepository.findAll(pageable)).thenReturn(mockPage);

        // when
        Page<LostCatPostDto> result = lostCatPostService.getAllLostCatPosts(pageable);

        // then
        assertThat(result.getContent().size()).isEqualTo(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("고양이 공고1");
        verify(lostCatRepository, times(1)).findAll(pageable);

    }

    @Test
    @DisplayName("고양이 찾기 글을 생성한다.")
    void createLostCatPost(){
        // given
        CreateLostCatPostRequest createLostCatPostRequest = CreateLostCatPostRequest.builder()
                .title("고양이 유기글 추가")
                .catAge(3)
                .catColor("검정")
                .build();

        LostCatPost lostCatPost = LostCatPost.convertToEntity(createLostCatPostRequest);
        when(lostCatRepository.save(any(LostCatPost.class))).thenReturn(lostCatPost);

        // when
        LostCatPostDto result = lostCatPostService.createLostCatPost(createLostCatPostRequest);

        // then
        assertThat(result.getTitle()).isEqualTo("고양이 유기글 추가");
        assertThat(result.getCatAge()).isEqualTo(3);
        assertThat(result.getCatColor()).isEqualTo("검정");
        verify(lostCatRepository, times(1)).save(any(LostCatPost.class));
    }

    @Test
    @DisplayName("고양이 찾기 글을 수정한다.")
    void updateLostCatPost(){
        // given
        Long id = 1L;
        UpdateLostCatPostRequest updateLostCatPostRequest = UpdateLostCatPostRequest.builder()
                .title("수정한 제목")
                .catType("아르비시안")
                .catWeight(8)
                .build();
        LostCatPost lostCatPost =  LostCatPost.builder()
                .lostCatPostId(1L)
                .title("수정전 제목")
                .catType("숏헤어")
                .catWeight(5)
                .build();
        when(lostCatRepository.findById(id)).thenReturn(Optional.of(lostCatPost));
        // when
        LostCatPostDto result = lostCatPostService.updateLostCatPost(id, updateLostCatPostRequest);
        // then
        assertThat(result.getTitle()).isEqualTo("수정한 제목");
        assertThat(result.getCatType()).isEqualTo("아르비시안");
        assertThat(result.getCatWeight()).isEqualTo(8);
        verify(lostCatRepository, times(1)).findById(anyLong());
    }

    @Test
    @DisplayName("고양이 찾기 글을 삭제한다.")
    void deleteLostCatPost(){
        // given
        Long lostCatPostId = 1L;
        when(lostCatRepository.existsById(lostCatPostId)).thenReturn(true);

        // when
        lostCatPostService.deleteLostCatPost(lostCatPostId);

        // then
        verify(lostCatRepository, times(1)).existsById(lostCatPostId);
        verify(lostCatRepository, times(1)).deleteById(lostCatPostId);
    }
}