package com.min.meow.comment.service;

import com.min.meow.comment.domain.dto.RegisterPostCommentDto;
import com.min.meow.comment.domain.dto.UpdatePostCommentDto;
import com.min.meow.comment.domain.request.RegisterPostCommentRequest;
import com.min.meow.comment.domain.request.UpdatePostCommentRequest;
import com.min.meow.comment.entity.PostComment;
import com.min.meow.comment.repository.CommentRepository;
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
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private LostCatRepository lostCatRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("댓글을 작성한다.")
    public void registerLostCatPostComment(){
        // given
        RegisterPostCommentRequest registerPostCommentRequest = RegisterPostCommentRequest.builder()
                .content("고양이를 발견했어요.")
                .build();
        LostCatPost lostCatPost = LostCatPost.builder()
                .id(1L)
                .content("고양이를 찾아요.")
                .build();
        PostComment postComment = PostComment.convertToEntity(registerPostCommentRequest, lostCatPost);
        when(lostCatRepository.findById(1L)).thenReturn(Optional.of(lostCatPost));
        // when
        RegisterPostCommentDto result = commentService.registerLostCatPostComment(registerPostCommentRequest, lostCatPost.getId());
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

        PostComment postComment = PostComment.builder()
                .lostCatPost(lostCatPost)
                .content("고양이를 발견했어요.")
                .build();

        UpdatePostCommentRequest updatePostCommentRequest = UpdatePostCommentRequest.builder()
                .content("죄송해요. 잘못봤어요.")
                .build();

        when(commentRepository.findById(lostCatPostCommentId)).thenReturn(Optional.of(postComment));

        // when
        UpdatePostCommentDto result = commentService.updateLostCatPostComment(updatePostCommentRequest, lostCatPostCommentId);

        // then
        assertThat(result.getContent()).isEqualTo("죄송해요. 잘못봤어요.");

    }

    @Test
    @DisplayName("댓글을 삭제한다.")
    void deleteLostCatPostComment(){
        // given
        Long lostCatPostCommentId = 1L;
        when(commentRepository.existsById(lostCatPostCommentId)).thenReturn(true);

        // when
        commentService.deleteLostCatPostComment(lostCatPostCommentId);

        // then
        verify(commentRepository, times(1)).existsById(lostCatPostCommentId);
        verify(commentRepository, times(1)).deleteById(lostCatPostCommentId);
    }

}