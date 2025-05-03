package com.min.meow.lostcatpostcomment.service;

import com.min.meow.lostcatpostcomment.domain.dto.RegisterLostCatPostCommentDto;
import com.min.meow.lostcatpostcomment.domain.dto.UpdateLostCatPostCommentDto;
import com.min.meow.lostcatpostcomment.domain.request.RegisterLostCatPostCommentRequest;
import com.min.meow.lostcatpostcomment.domain.request.UpdateLostCatPostCommentRequest;
import com.min.meow.lostcatpostcomment.entity.LostCatPostComment;
import com.min.meow.lostcatpostcomment.repository.LostCatPostCommentRepository;
import com.min.meow.lostcatpost.entity.LostCatPost;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class LostCatPostCommentServiceTest {

    @Mock
    private LostCatPostCommentRepository lostCatPostCommentRepository;

    @Mock
    private LostCatRepository lostCatRepository;

    @InjectMocks
    private LostCatPostCommentService lostCatPostCommentService;

    @Test
    @DisplayName("댓글을 작성한다.")
    public void registerLostCatPostComment(){
        // given
        RegisterLostCatPostCommentRequest registerLostCatPostCommentRequest = RegisterLostCatPostCommentRequest.builder()
                .content("고양이를 발견했어요.")
                .build();
        LostCatPost lostCatPost = LostCatPost.builder()
                .lostCatPostId(1L)
                .content("고양이를 찾아요.")
                .build();
        LostCatPostComment lostCatPostComment = LostCatPostComment.convertToEntity(registerLostCatPostCommentRequest, lostCatPost);
        when(lostCatRepository.findById(1L)).thenReturn(Optional.of(lostCatPost));
        // when
        RegisterLostCatPostCommentDto result = lostCatPostCommentService.registerLostCatPostComment(registerLostCatPostCommentRequest, lostCatPost.getLostCatPostId());
        // then
        assertThat(result).isNotNull();
        assertThat(result.getLostCatPostId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("고양이를 발견했어요.");
    }

    @Test
    @DisplayName("댓글을 수정한다.")
    void updateLostCatPostComment(){
        // given
        Long lostCatPostCommentId = 1L;

        LostCatPost lostCatPost = LostCatPost.builder()
                .title("고양이 찾기 글1")
                .build();

        LostCatPostComment lostCatPostComment = LostCatPostComment.builder()
                .lostCatPost(lostCatPost)
                .content("고양이를 발견했어요.")
                .build();

        UpdateLostCatPostCommentRequest updateLostCatPostCommentRequest = UpdateLostCatPostCommentRequest.builder()
                .content("죄송해요. 잘못봤어요.")
                .build();

        when(lostCatPostCommentRepository.findById(lostCatPostCommentId)).thenReturn(Optional.of(lostCatPostComment));

        // when
        UpdateLostCatPostCommentDto result = lostCatPostCommentService.updateLostCatPostComment(updateLostCatPostCommentRequest, lostCatPostCommentId);

        // then
        assertThat(result.getContent()).isEqualTo("죄송해요. 잘못봤어요.");

    }

    @Test
    @DisplayName("댓글을 삭제한다.")
    void deleteLostCatPostComment(){
        // given
        Long lostCatPostCommentId = 1L;
        when(lostCatPostCommentRepository.existsById(lostCatPostCommentId)).thenReturn(true);

        // when
        lostCatPostCommentService.deleteLostCatPostComment(lostCatPostCommentId);

        // then
        verify(lostCatPostCommentRepository, times(1)).existsById(lostCatPostCommentId);
        verify(lostCatPostCommentRepository, times(1)).deleteById(lostCatPostCommentId);
    }

}