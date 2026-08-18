package com.min.meow.comment.service;

import com.min.meow.comment.dto.request.RegisterCommentRequest;
import com.min.meow.comment.dto.request.UpdateCommentRequest;
import com.min.meow.comment.dto.response.RegisterCommentResponse;
import com.min.meow.comment.entity.Comment;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.common.PostType;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.notification.event.CommentEvent;
import com.min.meow.notification.event.NotificationEventPublisher;
import com.min.meow.notification.event.PopularScoreEvent;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 유닛 테스트")
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private LostCatRepository lostCatRepository;

    @Mock
    private BoastCatPostRepository boastCatPostRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private UserRepository userRepository;

    private User createUser(Long id) {
        return User.builder()
                .id(id)
                .loginId("user" + id)
                .nickname("냥이" + id)
                .password("$2a$10$encoded")
                .isDelete(false)
                .userRoles(new ArrayList<>())
                .build();
    }

    private BoastCatPost createBoastPost(Long id, User writer) {
        return BoastCatPost.builder().id(id).title("자랑글").contents("내용").user(writer).build();
    }

    private LostCatPost createLostPost(Long id, User writer) {
        return LostCatPost.builder().id(id).title("실종글").contents("내용").catName("나비")
                .lostLocation("서울").user(writer).build();
    }

    private Comment createComment(Long id, User author, Long postId, PostType postType, Comment parent, boolean deleted) {
        return Comment.builder()
                .id(id)
                .contents("댓글 내용")
                .user(author)
                .postId(postId)
                .postType(postType)
                .parentComment(parent)
                .isDeleted(deleted)
                .build();
    }

    @Nested
    @DisplayName("registerComment() — 댓글 작성")
    class RegisterComment {

        @Test
        @DisplayName("성공: 자랑글에 원댓글을 달면 댓글수를 증가시키고 인기점수 +2 이벤트를 발행한다")
        void test_성공_자랑글_원댓글_등록() {
            // given
            Long postId = 1L;
            Long writerId = 99L;
            Long commenterId = 10L;
            User writer = createUser(writerId);
            User commenter = createUser(commenterId);
            BoastCatPost post = createBoastPost(postId, writer);
            RegisterCommentRequest request = new RegisterCommentRequest();
            request.setContent("좋은 글이네요");

            given(userRepository.findById(commenterId)).willReturn(Optional.of(commenter));
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            RegisterCommentResponse response =
                    commentService.registerComment(request, postId, PostType.BOAST, commenterId);

            // then
            assertThat(response).isNotNull();
            then(boastCatPostRepository).should().incrementCommentCount(postId);
            then(notificationEventPublisher).should().publishCommentEvent(any(CommentEvent.class));

            var scoreCaptor = org.mockito.ArgumentCaptor.forClass(PopularScoreEvent.class);
            then(notificationEventPublisher).should().publishPopularScoreEvent(scoreCaptor.capture());
            assertThat(scoreCaptor.getValue().scoreDelta()).isEqualTo(2);
        }

        @Test
        @DisplayName("성공: 실종글에 댓글을 달면 인기점수 이벤트는 발행하지 않는다 (실종글은 인기 랭킹 대상 아님)")
        void test_성공_실종글_인기점수_이벤트_없음() {
            // given
            Long postId = 1L;
            User writer = createUser(99L);
            User commenter = createUser(10L);
            LostCatPost post = createLostPost(postId, writer);
            RegisterCommentRequest request = new RegisterCommentRequest();
            request.setContent("찾길 바라요");

            given(userRepository.findById(10L)).willReturn(Optional.of(commenter));
            given(lostCatRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            commentService.registerComment(request, postId, PostType.LOST, 10L);

            // then
            then(lostCatRepository).should().incrementCommentCount(postId);
            then(notificationEventPublisher).should(never()).publishPopularScoreEvent(any());
        }

        @Test
        @DisplayName("성공: 본인 글에 본인이 댓글을 달면 알림을 생략한다")
        void test_성공_본인_글_알림_생략() {
            // given
            Long postId = 1L;
            Long userId = 10L;
            User self = createUser(userId);
            BoastCatPost post = createBoastPost(postId, self);
            RegisterCommentRequest request = new RegisterCommentRequest();
            request.setContent("셀프 댓글");

            given(userRepository.findById(userId)).willReturn(Optional.of(self));
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            commentService.registerComment(request, postId, PostType.BOAST, userId);

            // then
            then(notificationEventPublisher).should(never()).publishCommentEvent(any());
        }

        @Test
        @DisplayName("성공: 작성자가 탈퇴한 게시글에 댓글을 달아도 알림을 생략한다")
        void test_성공_탈퇴한_작성자_알림_생략() {
            // given
            Long postId = 1L;
            User withdrawnWriter = User.builder()
                    .id(99L).loginId("writer").nickname("탈퇴함").password("pw")
                    .isDelete(true).userRoles(new ArrayList<>()).build();
            User commenter = createUser(10L);
            BoastCatPost post = createBoastPost(postId, withdrawnWriter);
            RegisterCommentRequest request = new RegisterCommentRequest();
            request.setContent("댓글");

            given(userRepository.findById(10L)).willReturn(Optional.of(commenter));
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            commentService.registerComment(request, postId, PostType.BOAST, 10L);

            // then
            then(notificationEventPublisher).should(never()).publishCommentEvent(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자면 NOT_FOUND_USER 예외를 던진다")
        void test_실패_존재하지_않는_사용자() {
            // given
            given(userRepository.findById(10L)).willReturn(Optional.empty());
            RegisterCommentRequest request = new RegisterCommentRequest();
            request.setContent("댓글");

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request, 1L, PostType.BOAST, 10L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_USER);
        }

        @Test
        @DisplayName("실패: 대댓글에 또 대댓글을 달면 COMMENT_DEPTH_EXCEEDED 예외를 던진다")
        void test_실패_2뎁스_초과() {
            // given
            Long postId = 1L;
            User writer = createUser(99L);
            User commenter = createUser(10L);
            Comment rootComment = createComment(1L, writer, postId, PostType.BOAST, null, false);
            Comment reply = createComment(2L, writer, postId, PostType.BOAST, rootComment, false);
            RegisterCommentRequest request = new RegisterCommentRequest();
            request.setContent("대대댓글 시도");
            request.setParentCommentId(2L);

            given(userRepository.findById(10L)).willReturn(Optional.of(commenter));
            given(commentRepository.findById(2L)).willReturn(Optional.of(reply));

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request, postId, PostType.BOAST, 10L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COMMENT_DEPTH_EXCEEDED);
        }

        @Test
        @DisplayName("실패: 삭제된 댓글을 부모로 지정하면 NOT_FOUND_COMMENT 예외를 던진다")
        void test_실패_삭제된_부모_댓글() {
            // given
            Long postId = 1L;
            User writer = createUser(99L);
            User commenter = createUser(10L);
            Comment deletedRoot = createComment(1L, writer, postId, PostType.BOAST, null, true);
            RegisterCommentRequest request = new RegisterCommentRequest();
            request.setContent("대댓글");
            request.setParentCommentId(1L);

            given(userRepository.findById(10L)).willReturn(Optional.of(commenter));
            given(commentRepository.findById(1L)).willReturn(Optional.of(deletedRoot));

            // when & then
            assertThatThrownBy(() -> commentService.registerComment(request, postId, PostType.BOAST, 10L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_COMMENT);
        }

        @Test
        @DisplayName("실패: 다른 게시글의 댓글을 부모로 지정하면 NOT_FOUND_COMMENT 예외를 던진다")
        void test_실패_다른_게시글_댓글을_부모로_지정() {
            // given
            User writer = createUser(99L);
            User commenter = createUser(10L);
            Comment otherPostRoot = createComment(1L, writer, 999L, PostType.BOAST, null, false);
            RegisterCommentRequest request = new RegisterCommentRequest();
            request.setContent("대댓글");
            request.setParentCommentId(1L);

            given(userRepository.findById(10L)).willReturn(Optional.of(commenter));
            given(commentRepository.findById(1L)).willReturn(Optional.of(otherPostRoot));

            // when & then — 부모는 postId=999인데 현재 postId=1로 요청
            assertThatThrownBy(() -> commentService.registerComment(request, 1L, PostType.BOAST, 10L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_COMMENT);
        }
    }

    @Nested
    @DisplayName("updateComment() — 댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("성공: 본인 댓글이면 내용을 수정할 수 있다")
        void test_성공_본인_댓글_수정() {
            // given
            Long userId = 10L;
            User author = createUser(userId);
            Comment comment = createComment(1L, author, 1L, PostType.BOAST, null, false);
            UpdateCommentRequest request = new UpdateCommentRequest();
            request.setContent("수정된 댓글");

            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when
            commentService.updateComment(request, 1L, userId);

            // then
            assertThat(comment.getContents()).isEqualTo("수정된 댓글");
        }

        @Test
        @DisplayName("실패: 삭제된 댓글은 수정할 수 없다")
        void test_실패_삭제된_댓글_수정() {
            // given
            Long userId = 10L;
            User author = createUser(userId);
            Comment deletedComment = createComment(1L, author, 1L, PostType.BOAST, null, true);
            UpdateCommentRequest request = new UpdateCommentRequest();
            request.setContent("수정 시도");

            given(commentRepository.findById(1L)).willReturn(Optional.of(deletedComment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(request, 1L, userId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_COMMENT);
        }

        @Test
        @DisplayName("실패: 타인 댓글은 수정할 수 없다")
        void test_실패_타인_댓글_수정() {
            // given
            User author = createUser(1L);
            Comment comment = createComment(1L, author, 1L, PostType.BOAST, null, false);
            UpdateCommentRequest request = new UpdateCommentRequest();
            request.setContent("수정 시도");

            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(request, 1L, 2L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }
    }

    @Nested
    @DisplayName("deleteComment() — 댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("성공: 활성 대댓글이 없는 원댓글은 즉시 삭제되고 게시글 댓글수가 감소한다")
        void test_성공_대댓글_없는_원댓글_즉시_삭제() {
            // given
            Long userId = 10L;
            User author = createUser(userId);
            Comment root = createComment(1L, author, 5L, PostType.BOAST, null, false);

            given(commentRepository.findById(1L)).willReturn(Optional.of(root));
            given(commentRepository.countActiveRepliesByParentId(1L)).willReturn(0L);

            // when
            commentService.deleteComment(1L, userId, false);

            // then
            then(commentRepository).should().delete(root);
            then(boastCatPostRepository).should().decrementCommentCount(5L);
        }

        @Test
        @DisplayName("성공: 활성 대댓글이 있는 원댓글은 즉시 삭제하지 않고 소프트 삭제한다")
        void test_성공_대댓글_있는_원댓글_소프트_삭제() {
            // given
            Long userId = 10L;
            User author = createUser(userId);
            Comment root = createComment(1L, author, 5L, PostType.BOAST, null, false);

            given(commentRepository.findById(1L)).willReturn(Optional.of(root));
            given(commentRepository.countActiveRepliesByParentId(1L)).willReturn(2L);

            // when
            commentService.deleteComment(1L, userId, false);

            // then
            assertThat(root.isDeleted()).isTrue();
            assertThat(root.getContents()).isEqualTo("삭제된 댓글입니다.");
            then(commentRepository).should(never()).delete(root);
            then(boastCatPostRepository).should(never()).decrementCommentCount(any());
        }

        @Test
        @DisplayName("성공: 대댓글 삭제 후 부모가 소프트 삭제 상태이고 활성 대댓글이 0개면 부모도 함께 삭제된다")
        void test_성공_대댓글_삭제시_부모_연쇄_삭제() {
            // given
            Long userId = 10L;
            User author = createUser(userId);
            Comment softDeletedParent = createComment(1L, author, 5L, PostType.BOAST, null, true);
            Comment reply = createComment(2L, author, 5L, PostType.BOAST, softDeletedParent, false);

            given(commentRepository.findById(2L)).willReturn(Optional.of(reply));
            given(commentRepository.countActiveRepliesByParentId(1L)).willReturn(0L);

            // when
            commentService.deleteComment(2L, userId, false);

            // then — 대댓글 삭제 + 부모 연쇄 삭제로 댓글수가 2번 감소
            then(commentRepository).should().delete(reply);
            then(commentRepository).should().delete(softDeletedParent);
            then(boastCatPostRepository).should(org.mockito.Mockito.times(2)).decrementCommentCount(5L);
        }

        @Test
        @DisplayName("성공: 대댓글 삭제 후 부모가 아직 소프트 삭제 상태가 아니면 부모는 삭제되지 않는다")
        void test_성공_대댓글_삭제시_활성_부모는_유지() {
            // given
            Long userId = 10L;
            User author = createUser(userId);
            Comment activeParent = createComment(1L, author, 5L, PostType.BOAST, null, false);
            Comment reply = createComment(2L, author, 5L, PostType.BOAST, activeParent, false);

            given(commentRepository.findById(2L)).willReturn(Optional.of(reply));

            // when
            commentService.deleteComment(2L, userId, false);

            // then
            then(commentRepository).should().delete(reply);
            then(commentRepository).should(never()).delete(activeParent);
            then(commentRepository).should(never()).countActiveRepliesByParentId(any());
        }

        @Test
        @DisplayName("실패: 작성자도 아니고 comment:delete 권한도 없으면 FORBIDDEN_NOT_AUTHOR 예외를 던진다")
        void test_실패_권한_없는_삭제_차단() {
            // given
            User author = createUser(1L);
            Comment comment = createComment(1L, author, 5L, PostType.BOAST, null, false);
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(1L, 2L, false))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN_NOT_AUTHOR);

            then(commentRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("성공: 작성자가 아니어도 comment:delete 권한이 있으면 삭제할 수 있다")
        void test_성공_권한으로_타인_댓글_삭제() {
            // given
            User author = createUser(1L);
            Comment comment = createComment(1L, author, 5L, PostType.BOAST, null, false);
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
            given(commentRepository.countActiveRepliesByParentId(1L)).willReturn(0L);

            // when
            commentService.deleteComment(1L, 2L, true);

            // then
            then(commentRepository).should().delete(comment);
        }
    }
}
