package com.min.meow.postlike.service;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.notification.event.LikeEvent;
import com.min.meow.notification.event.NotificationEventPublisher;
import com.min.meow.notification.event.PopularScoreEvent;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.postlike.entity.PostLike;
import com.min.meow.postlike.repository.PostLikeRepository;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostLikeService 유닛 테스트")
class PostLikeServiceTest {

    @InjectMocks
    private PostLikeService postLikeService;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private BoastCatPostRepository boastCatPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private User createUser(Long id, boolean withdrawn) {
        return User.builder()
                .id(id)
                .loginId("user" + id)
                .nickname("냥이" + id)
                .password("$2a$10$encoded")
                .isDelete(withdrawn)
                .userRoles(new ArrayList<>())
                .build();
    }

    private BoastCatPost createPost(Long id, User writer, int likeCount) {
        return BoastCatPost.builder()
                .id(id)
                .title("자랑글")
                .contents("우리 고양이 자랑")
                .user(writer)
                .likeCount(likeCount)
                .build();
    }

    @Nested
    @DisplayName("addLike() — 좋아요 등록")
    class AddLike {

        @Test
        @DisplayName("성공: 정상 좋아요 시 좋아요를 저장하고 카운트를 1 증가시킨 값을 반환한다")
        void test_성공_좋아요_등록() {
            // given
            Long postId = 1L;
            Long userId = 10L;
            Long writerId = 99L;
            User writer = createUser(writerId, false);
            User liker = createUser(userId, false);
            BoastCatPost post = createPost(postId, writer, 3);

            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(false);
            given(boastCatPostRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));
            given(userRepository.findById(userId)).willReturn(Optional.of(liker));

            // when
            Long result = postLikeService.addLike(postId, userId);

            // then
            assertThat(result).isEqualTo(4L);
            then(postLikeRepository).should().save(any(PostLike.class));
            then(boastCatPostRepository).should().updateLikeCount(postId, 1);
            then(notificationEventPublisher).should().publishLikeEvent(any(LikeEvent.class));

            ArgumentCaptor<PopularScoreEvent> scoreCaptor = ArgumentCaptor.forClass(PopularScoreEvent.class);
            then(notificationEventPublisher).should().publishPopularScoreEvent(scoreCaptor.capture());
            assertThat(scoreCaptor.getValue().postId()).isEqualTo(postId);
            assertThat(scoreCaptor.getValue().scoreDelta()).isEqualTo(3);
        }

        @Test
        @DisplayName("실패: 이미 좋아요를 누른 게시글이면 ALREADY_LIKED 예외를 던지고 아무 것도 저장하지 않는다")
        void test_실패_이미_좋아요_누름() {
            // given
            Long postId = 1L;
            Long userId = 10L;
            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> postLikeService.addLike(postId, userId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_LIKED);

            then(postLikeRepository).should(never()).save(any());
            then(boastCatPostRepository).should(never()).updateLikeCount(any(), anyInt());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 게시글이면 NOT_FOUND_POST 예외를 던진다")
        void test_실패_존재하지_않는_게시글() {
            // given
            Long postId = 999L;
            Long userId = 10L;
            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(false);
            given(boastCatPostRepository.findByIdWithUser(postId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postLikeService.addLike(postId, userId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_POST);
        }

        @Test
        @DisplayName("실패: 탈퇴한 회원이면 NOT_FOUND_USER 예외를 던지고 좋아요를 저장하지 않는다")
        void test_실패_탈퇴한_회원() {
            // given
            Long postId = 1L;
            Long userId = 10L;
            User writer = createUser(99L, false);
            User withdrawnUser = createUser(userId, true);
            BoastCatPost post = createPost(postId, writer, 0);

            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(false);
            given(boastCatPostRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));
            given(userRepository.findById(userId)).willReturn(Optional.of(withdrawnUser));

            // when & then
            assertThatThrownBy(() -> postLikeService.addLike(postId, userId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_USER);

            then(postLikeRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("성공: 동시 요청으로 UNIQUE 제약이 위반되면 ALREADY_LIKED로 변환한다")
        void test_성공_동시요청_UNIQUE_위반_변환() {
            // given
            Long postId = 1L;
            Long userId = 10L;
            User writer = createUser(99L, false);
            User liker = createUser(userId, false);
            BoastCatPost post = createPost(postId, writer, 0);

            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(false);
            given(boastCatPostRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));
            given(userRepository.findById(userId)).willReturn(Optional.of(liker));
            given(postLikeRepository.save(any(PostLike.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate"));

            // when & then
            assertThatThrownBy(() -> postLikeService.addLike(postId, userId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_LIKED);
        }

        @Test
        @DisplayName("성공: 본인 게시글에 좋아요를 눌러도 등록은 되지만 알림은 발행하지 않는다")
        void test_성공_본인_게시글_알림_생략() {
            // given
            Long postId = 1L;
            Long userId = 10L;
            User self = createUser(userId, false);
            BoastCatPost post = createPost(postId, self, 0);

            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(false);
            given(boastCatPostRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));
            given(userRepository.findById(userId)).willReturn(Optional.of(self));

            // when
            postLikeService.addLike(postId, userId);

            // then
            then(notificationEventPublisher).should(never()).publishLikeEvent(any());
            // 인기글 점수는 본인 글이어도 그대로 반영됨
            then(notificationEventPublisher).should().publishPopularScoreEvent(any());
        }

        @Test
        @DisplayName("성공: 작성자가 탈퇴한 게시글에 좋아요를 눌러도 알림은 발행하지 않는다")
        void test_성공_탈퇴한_작성자_알림_생략() {
            // given
            Long postId = 1L;
            Long userId = 10L;
            User withdrawnWriter = createUser(99L, true);
            User liker = createUser(userId, false);
            BoastCatPost post = createPost(postId, withdrawnWriter, 0);

            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(false);
            given(boastCatPostRepository.findByIdWithUser(postId)).willReturn(Optional.of(post));
            given(userRepository.findById(userId)).willReturn(Optional.of(liker));

            // when
            postLikeService.addLike(postId, userId);

            // then
            then(notificationEventPublisher).should(never()).publishLikeEvent(any());
        }
    }

    @Nested
    @DisplayName("cancelLike() — 좋아요 취소")
    class CancelLike {

        @Test
        @DisplayName("성공: 좋아요를 취소하면 카운트를 1 감소시킨 값을 반환한다")
        void test_성공_좋아요_취소() {
            // given
            Long postId = 1L;
            Long userId = 10L;
            User writer = createUser(99L, false);
            BoastCatPost post = createPost(postId, writer, 5);

            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(true);
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            Long result = postLikeService.cancelLike(postId, userId);

            // then
            assertThat(result).isEqualTo(4L);
            then(postLikeRepository).should().deleteByBoastCatPostIdAndUserId(postId, userId);
            then(boastCatPostRepository).should().updateLikeCount(postId, -1);

            ArgumentCaptor<PopularScoreEvent> scoreCaptor = ArgumentCaptor.forClass(PopularScoreEvent.class);
            then(notificationEventPublisher).should().publishPopularScoreEvent(scoreCaptor.capture());
            assertThat(scoreCaptor.getValue().scoreDelta()).isEqualTo(-3);
        }

        @Test
        @DisplayName("실패: 좋아요를 누르지 않은 게시글을 취소하면 NOT_LIKED 예외를 던진다")
        void test_실패_좋아요_안_누름() {
            // given
            Long postId = 1L;
            Long userId = 10L;
            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> postLikeService.cancelLike(postId, userId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_LIKED);

            then(boastCatPostRepository).should(never()).updateLikeCount(any(), anyInt());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 게시글이면 NOT_FOUND_POST 예외를 던진다")
        void test_실패_존재하지_않는_게시글() {
            // given
            Long postId = 999L;
            Long userId = 10L;
            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(true);
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postLikeService.cancelLike(postId, userId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_POST);
        }

        @Test
        @DisplayName("성공: 좋아요 수가 0인 상태에서 취소해도 음수로 내려가지 않고 0을 반환한다")
        void test_성공_좋아요_수_0_하한선_보장() {
            // given — JPQL UPDATE가 1차 캐시를 우회해 post.getLikeCount()가 갱신 전 값(0)을 그대로 들고 있는 상황을 재현
            Long postId = 1L;
            Long userId = 10L;
            User writer = createUser(99L, false);
            BoastCatPost post = createPost(postId, writer, 0);

            given(postLikeRepository.existsByBoastCatPostIdAndUserId(postId, userId)).willReturn(true);
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            Long result = postLikeService.cancelLike(postId, userId);

            // then
            assertThat(result).isZero();
        }
    }
}
