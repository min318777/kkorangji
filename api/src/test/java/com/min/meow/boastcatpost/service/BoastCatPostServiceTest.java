package com.min.meow.boastcatpost.service;

import com.min.meow.boastcatpost.domain.dto.BoastCatPostDto;
import com.min.meow.boastcatpost.domain.dto.CreateBoastCatPostDto;
import com.min.meow.boastcatpost.domain.dto.UpdateBoastCatPostDto;
import com.min.meow.boastcatpost.domain.request.CreateBoastCatPostRequest;
import com.min.meow.boastcatpost.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.boastcatpost.entity.BoastCatPost;
import com.min.meow.boastcatpost.repository.BoastCatPostRepository;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class BoastCatPostServiceTest {

    @Mock
    private BoastCatPostRepository boastCatPostRepository;

    @InjectMocks
    private BoastCatPostService boastCatPostService;

    @Test
    @DisplayName("모든 고양이 자랑글을 조회한다.")
    void getAllBoastCatPosts(){

        // given
        Pageable pageable = PageRequest.of(0,10);
        List<BoastCatPost> posts = List.of(BoastCatPost.builder().title("고양이 자랑글1").build(),
                                            BoastCatPost.builder().title("고양이 자랑글2").build());
        Page<BoastCatPost> mockPage = new PageImpl<>(posts, pageable, posts.size());
        when(boastCatPostRepository.findAll(pageable)).thenReturn(mockPage);

        // when
        Page<BoastCatPostDto> result = boastCatPostService.getAllBoastCatPosts(pageable);

        // then
        assertThat(result.getContent().size()).isEqualTo(2);
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("고양이 자랑글2");
        verify(boastCatPostRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("고양이 자랑글을 생성한다.")
    void createBoastCatPost(){
        // given
        CreateBoastCatPostRequest createBoastCatPostRequest = CreateBoastCatPostRequest.builder()
                .title("제 고양이를 자랑합니다.")
                .content("제 고양이는 예쁜 눈을 가졌습니다.")
                .build();
        BoastCatPost boastCatPost = BoastCatPost.builder()
                .title("제 고양이를 자랑합니다.")
                .content("제 고양이는 예쁜 눈을 가졌습니다.")
                .build();
        when(boastCatPostRepository.save(any(BoastCatPost.class))).thenReturn(boastCatPost);

        // when
        CreateBoastCatPostDto result = boastCatPostService.createBoastCatPost(createBoastCatPostRequest);

        // then
        assertThat(result.getTitle()).isEqualTo(boastCatPost.getTitle());
        assertThat(result.getContent()).isEqualTo("제 고양이는 예쁜 눈을 가졌습니다.");
        verify(boastCatPostRepository, times(1)).save(any(BoastCatPost.class));
    }

    @Test
    @DisplayName("고양이 자랑글을 수정 한다.")
    void updateBoastCatPost(){
        // given
        Long boastCatPostId = 2L;
        UpdateBoastCatPostRequest updateBoastCatPostRequest = UpdateBoastCatPostRequest.builder()
                .title("자랑글을 수정하겠습니다.")
                .content("수정된 자랑글 내용")
                .build();
        BoastCatPost boastCatPost = BoastCatPost.builder()
                .title("고양이 자랑글 1")
                .content("수정전 자랑글 내용")
                .build();
        when(boastCatPostRepository.findById(boastCatPostId)).thenReturn(Optional.of(boastCatPost));

        // when
        UpdateBoastCatPostDto result = boastCatPostService.updateBoastCatPost(updateBoastCatPostRequest, boastCatPostId);

        // then
        assertThat(boastCatPost.getTitle()).isEqualTo(result.getTitle());
        assertThat(result.getTitle()).isEqualTo("자랑글을 수정하겠습니다.");
        assertThat(result.getContent()).isEqualTo("수정된 자랑글 내용");

    }

    @Test
    @DisplayName("고양이 자랑글을 삭제를 한다.")
    void deleteBoastCatPost(){
        // given
        Long boastCatPostId = 1L;
        when(boastCatPostRepository.existsById(boastCatPostId)).thenReturn(true);
        // when
        boastCatPostService.deleteBoastCatPost(boastCatPostId);

        // then
        verify(boastCatPostRepository, times(1)).existsById(boastCatPostId);
        verify(boastCatPostRepository, times(1)).deleteById(boastCatPostId);
    }
}