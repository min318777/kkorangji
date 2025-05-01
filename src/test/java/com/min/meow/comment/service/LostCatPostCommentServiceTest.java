package com.min.meow.comment.service;

import com.min.meow.comment.domain.dto.RegisterCommentDto;
import com.min.meow.comment.domain.request.RegisterCommentRequest;
import com.min.meow.comment.entity.LostCatPostComment;
import com.min.meow.comment.repository.LostCatPostCommentRepository;
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
import static org.mockito.Mockito.when;


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
        RegisterCommentRequest registerCommentRequest = RegisterCommentRequest.builder()
                .content("고양이를 발견했어요.")
                .build();
        LostCatPost lostCatPost = LostCatPost.builder()
                .lostCatPostId(1L)
                .content("고양이를 찾아요.")
                .build();
        LostCatPostComment lostCatPostComment = LostCatPostComment.convertToEntity(registerCommentRequest, lostCatPost);
        when(lostCatRepository.findById(1L)).thenReturn(Optional.of(lostCatPost));
        // when
        RegisterCommentDto result = lostCatPostCommentService.registerComment(registerCommentRequest, lostCatPost.getLostCatPostId());
        // then
        assertThat(result).isNotNull();
        assertThat(result.getLostCatPostId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("고양이를 발견했어요.");
    }

}