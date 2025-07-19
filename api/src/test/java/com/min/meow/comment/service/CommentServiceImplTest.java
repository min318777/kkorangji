package com.min.meow.comment.service;

import com.min.meow.post.comment.domain.response.RegisterCommentDto;
import com.min.meow.post.comment.domain.response.UpdateCommentDto;
import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.entity.Comment;
import com.min.meow.post.comment.repository.CommentRepository;
import com.min.meow.post.comment.service.impl.CommentServiceImpl;
import com.min.meow.post.lostcatpost.entity.LostCatPost;
import com.min.meow.post.lostcatpost.repository.LostCatRepository;
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
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private LostCatRepository lostCatRepository;

    @InjectMocks
    private CommentServiceImpl commentServiceImpl;

    @Test
    @DisplayName("댓글을 작성한다.")
    public void registerLostCatPostComment(){
        // given
        RegisterCommentRequest registerCommentRequest = RegisterCommentRequest.builder()
                .content("고양이를 발견했어요.")
                .build();
        LostCatPost lostCatPost = LostCatPost.builder()
                .id(1L)
                .contents("고양이를 찾아요.")
                .build();
        Comment comment = Comment.convertToEntity(registerCommentRequest, lostCatPost);
        when(lostCatRepository.findById(1L)).thenReturn(Optional.of(lostCatPost));
        // when
        RegisterCommentDto result = commentServiceImpl.registerLostCatPostComment(registerCommentRequest, lostCatPost.getId());
        // then
        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(1L);
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

        Comment comment = Comment.builder()
                .lostCatPost(lostCatPost)
                .contents("고양이를 발견했어요.")
                .build();

        UpdateCommentRequest updateCommentRequest = UpdateCommentRequest.builder()
                .content("죄송해요. 잘못봤어요.")
                .build();

        when(commentRepository.findById(lostCatPostCommentId)).thenReturn(Optional.of(comment));

        // when
        UpdateCommentDto result = commentServiceImpl.updateLostCatPostComment(updateCommentRequest, lostCatPostCommentId);

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
        commentServiceImpl.deleteLostCatPostComment(lostCatPostCommentId);

        // then
        verify(commentRepository, times(1)).existsById(lostCatPostCommentId);
        verify(commentRepository, times(1)).deleteById(lostCatPostCommentId);
    }

}