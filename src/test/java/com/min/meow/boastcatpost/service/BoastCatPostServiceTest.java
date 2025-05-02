package com.min.meow.boastcatpost.service;

import com.min.meow.boastcatpost.domain.dto.BoastCatPostDto;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class BoastCatPostServiceTest {

    @Mock
    private BoastCatPostRepository boastCatPostRepository;

    @InjectMocks
    private BoastCatPostService boastCatPostService;

    @Test
    @DisplayName("모든 고양이 자랑글을 조회한다.")
    public void getAllBoastCatPosts(){

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
}