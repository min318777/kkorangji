package com.min.meow.lostcatpost.service;

import com.min.meow.lostcatpost.domain.dto.LostCatPostDto;
import com.min.meow.lostcatpost.domain.entity.LostCatPost;
import com.min.meow.lostcatpost.repository.LostCatRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class LostCatPostServiceTest {

    @Mock
    private LostCatRepository lostCatRepository;

    @InjectMocks
    private LostCatPostService lostCatPostService;

    @Test
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
}