package com.min.meow.lostcatpost.service;

import com.min.meow.lostcatpost.domain.dto.CreateLostCatPostResponse;
import com.min.meow.lostcatpost.domain.dto.UpdateLostCatPostResponse;
import com.min.meow.lostcatpost.entity.LostCatPost;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import com.min.meow.comment.entity.PostComment;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
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
class LostCatPostServiceImplTest {

    @Mock
    private LostCatRepository lostCatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private LostCatPostServiceImpl lostCatPostServiceImpl;

    @Test
    @DisplayName("모든 고양이 찾기 글을 조회한다.")
    void getAllLostCatPost(){
        // given
        Pageable pageable = PageRequest.of(0, 10);
        PostComment postComment = PostComment.builder()
                .content("고양이를 발견했어요.")
                .build();
        List<LostCatPost> posts = List.of(LostCatPost.builder().title("고양이 공고1").postComments(List.of(postComment)).build(),
                                            LostCatPost.builder().title("고양이 공고2").postComments(List.of(postComment)).build());

        Page<LostCatPost> mockPage = new PageImpl<>(posts, pageable, posts.size());

        when(lostCatRepository.findAll(pageable)).thenReturn(mockPage);

        // when
        Page<UpdateLostCatPostResponse> result = lostCatPostServiceImpl.getAllLostCatPosts(pageable);

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
        User user = User.builder()
                .loginId("tempId")
                .build();
        LostCatPost lostCatPost = LostCatPost.convertToEntity(createLostCatPostRequest, user);
        when(lostCatRepository.save(any(LostCatPost.class))).thenReturn(lostCatPost);

        // when
        CreateLostCatPostResponse result = lostCatPostServiceImpl.createLostCatPost(createLostCatPostRequest, user.getLoginId());
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
        PostComment postComment = PostComment.builder()
                .content("고양이를 발견했어요.")
                .build();

        UpdateLostCatPostRequest updateLostCatPostRequest = UpdateLostCatPostRequest.builder()
                .title("수정한 제목")
                .catType("아르비시안")
                .catWeight(8)
                .build();

        LostCatPost lostCatPost =  LostCatPost.builder()
                .id(1L)
                .title("수정전 제목")
                .catType("숏헤어")
                .postComments(List.of(postComment))
                .catWeight(5)
                .build();
        String loginId = "tempId";

        when(lostCatRepository.findById(id)).thenReturn(Optional.of(lostCatPost));

        // when
        UpdateLostCatPostResponse result = lostCatPostServiceImpl.updateLostCatPost(id, updateLostCatPostRequest, loginId);

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
        String password = "password";
        when(lostCatRepository.existsById(lostCatPostId)).thenReturn(true);

        // when
        lostCatPostServiceImpl.deleteLostCatPost(lostCatPostId , "lostCatPostId", password);

        // then
        verify(lostCatRepository, times(1)).existsById(lostCatPostId);
        verify(lostCatRepository, times(1)).deleteById(lostCatPostId);
    }
}