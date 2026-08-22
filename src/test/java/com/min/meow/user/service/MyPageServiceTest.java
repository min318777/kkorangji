package com.min.meow.user.service;

import com.min.meow.comment.entity.Comment;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.common.PostType;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.common.PageResponse;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.postlike.repository.PostLikeRepository;
import com.min.meow.user.dto.response.MyCommentDto;
import com.min.meow.user.dto.response.MyPageSummaryResponse;
import com.min.meow.user.dto.response.MyPostDto;
import com.min.meow.user.dto.request.UpdateProfileRequest;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyPageService 유닛 테스트")
class MyPageServiceTest {

    /** 테스트 대상 — 모든 의존성은 아래 @Mock으로 자동 주입 */
    @InjectMocks
    private MyPageService myPageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoastCatPostRepository boastCatPostRepository;

    @Mock
    private LostCatRepository lostCatRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    /** 기본 테스트용 User 객체 생성 (DB 없이 순수 Java 객체) */
    private User createTestUser(Long id, String loginId) {
        return User.builder()
                .loginId(loginId)
                .email(loginId + "@test.com")
                .nickname("테스트닉네임")
                .password("encoded")
                .userRoles(new ArrayList<>())
                .build();
        // Note: id는 @GeneratedValue이므로 직접 설정 불가 — Mock에서 stub으로 처리
    }

    @Nested
    @DisplayName("getMyPageSummary — 마이페이지 요약 조회")
    class GetMyPageSummary {

        @Test
        @DisplayName("성공: 사용자가 존재하면 통계 포함 요약 정보를 반환한다")
        void test_성공_마이페이지_요약_조회() {
            // given — User Mock 설정
            User user = createTestUser(1L, "testuser");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.countBoastCatPostsByUserId(any())).willReturn(10L);
            given(userRepository.countLostCatPostsByUserId(any())).willReturn(5L);
            given(commentRepository.countByUserId(any())).willReturn(30L);

            // when
            MyPageSummaryResponse response = myPageService.getMyPageSummary(1L);

            // then — totalPostCount = boastCount + lostCount = 10 + 5 = 15
            assertThat(response.getLoginId()).isEqualTo("testuser");
            assertThat(response.getBoastCatPostCount()).isEqualTo(10L);
            assertThat(response.getLostCatPostCount()).isEqualTo(5L);
            assertThat(response.getTotalPostCount()).isEqualTo(15L);
            assertThat(response.getTotalCommentCount()).isEqualTo(30L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 userId로 조회하면 CustomException(NOT_FOUND_USER)을 던진다")
        void test_실패_존재하지_않는_사용자() {
            // given — 사용자 없음
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then — MyPageService.getMyPageSummary()는 NOT_FOUND_USER를 던짐
            // (UNREGISTERED_USER는 UserService.login()에서만 사용됨)
            assertThatThrownBy(() -> myPageService.getMyPageSummary(999L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_USER);
        }

        @Test
        @DisplayName("성공: 게시글과 댓글이 없는 신규 사용자는 모든 카운트가 0이다")
        void test_성공_게시글_없는_신규_사용자() {
            // given — 모든 카운트 0
            User user = createTestUser(2L, "newuser");
            given(userRepository.findById(2L)).willReturn(Optional.of(user));
            given(userRepository.countBoastCatPostsByUserId(any())).willReturn(0L);
            given(userRepository.countLostCatPostsByUserId(any())).willReturn(0L);
            given(commentRepository.countByUserId(any())).willReturn(0L);

            // when
            MyPageSummaryResponse response = myPageService.getMyPageSummary(2L);

            // then
            assertThat(response.getTotalPostCount()).isEqualTo(0L);
            assertThat(response.getBoastCatPostCount()).isEqualTo(0L);
            assertThat(response.getLostCatPostCount()).isEqualTo(0L);
            assertThat(response.getTotalCommentCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("updateProfile — 프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("성공: 닉네임 수정 시 user.updateNickname()이 호출된다")
        void test_성공_닉네임_수정() {
            // given
            User user = spy(createTestUser(1L, "testuser")); // spy로 메서드 호출 검증
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.countBoastCatPostsByUserId(any())).willReturn(0L);
            given(userRepository.countLostCatPostsByUserId(any())).willReturn(0L);
            given(commentRepository.countByUserId(any())).willReturn(0L);

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("새닉네임");

            // when
            myPageService.updateProfile(1L, request);

            // then — updateNickname 도메인 메서드가 정확히 1회 호출되었는지 검증
            verify(user, times(1)).updateNickname("새닉네임");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 수정 시 CustomException(NOT_FOUND_USER)을 던진다")
        void test_실패_존재하지_않는_사용자_수정() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("닉네임");

            // when & then
            assertThatThrownBy(() -> myPageService.updateProfile(999L, request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_USER);
        }

        @Test
        @DisplayName("성공: 수정 후 반환된 응답에 새 닉네임이 반영된다")
        void test_성공_수정_후_응답에_새_닉네임_반영() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.countBoastCatPostsByUserId(any())).willReturn(0L);
            given(userRepository.countLostCatPostsByUserId(any())).willReturn(0L);
            given(commentRepository.countByUserId(any())).willReturn(0L);

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("새닉네임");

            // when — user.updateNickname() 실제 호출로 닉네임 변경
            MyPageSummaryResponse response = myPageService.updateProfile(1L, request);

            // then — 변경된 닉네임이 응답에 반영
            assertThat(response.getNickname()).isEqualTo("새닉네임");
        }
    }

    @Nested
    @DisplayName("getMyPosts — 내가 쓴 게시글 목록 조회")
    class GetMyPosts {

        @Test
        @DisplayName("실패: PostType.ALL 조회 시 CustomException(INVALID_POST_TYPE)을 던진다")
        void test_실패_전체_타입_조회_미지원() {
            // given — ALL 타입은 OOM 위험으로 미지원 (전체 메모리 로딩 불가)
            User user = createTestUser(1L, "testuser");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            Pageable pageable = PageRequest.of(0, 10);

            // when & then — INVALID_POST_TYPE 예외 발생
            assertThatThrownBy(() -> myPageService.getMyPosts(1L, pageable, PostType.ALL))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_POST_TYPE);

            // repository가 호출되지 않아야 함
            verify(boastCatPostRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
            verify(lostCatRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
        }

        @Test
        @DisplayName("성공: PostType.BOAST 조회 시 BoastCatPostRepository만 호출된다")
        void test_성공_자랑글만_조회() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            Page<BoastCatPost> boastPage = new PageImpl<>(Collections.emptyList());
            Pageable pageable = PageRequest.of(0, 10);
            given(boastCatPostRepository.findByUserIdOrderByCreatedAtDesc(any(), eq(pageable)))
                    .willReturn(boastPage);

            // when
            myPageService.getMyPosts(1L, pageable, PostType.BOAST);

            // then — boast만 조회, lost는 호출되지 않아야 함
            verify(boastCatPostRepository).findByUserIdOrderByCreatedAtDesc(any(), eq(pageable));
            verify(lostCatRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
        }

        @Test
        @DisplayName("성공: PostType.LOST 조회 시 LostCatRepository만 호출된다")
        void test_성공_실종글만_조회() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            Page<LostCatPost> lostPage = new PageImpl<>(Collections.emptyList());
            Pageable pageable = PageRequest.of(0, 10);
            given(lostCatRepository.findByUserIdOrderByCreatedAtDesc(any(), eq(pageable)))
                    .willReturn(lostPage);

            // when
            myPageService.getMyPosts(1L, pageable, PostType.LOST);

            // then — lost만 조회, boast는 호출되지 않아야 함
            verify(lostCatRepository).findByUserIdOrderByCreatedAtDesc(any(), eq(pageable));
            verify(boastCatPostRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
        }
    }

    @Nested
    @DisplayName("getMyComments — 내가 쓴 댓글 목록 조회")
    class GetMyComments {

        @Test
        @DisplayName("성공: 댓글 목록을 페이징하여 반환한다")
        void test_성공_댓글_목록_조회() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            // Comment 엔티티는 postId + postType 구조 (boastCatPost FK 제거됨)
            Comment comment1 = mock(Comment.class);
            given(comment1.getId()).willReturn(1L);
            given(comment1.getContents()).willReturn("첫 번째 댓글");
            given(comment1.getPostType()).willReturn(PostType.BOAST);
            given(comment1.getPostId()).willReturn(10L);
            given(comment1.getCreatedAt()).willReturn(now);
            given(comment1.getUpdatedAt()).willReturn(now);

            Comment comment2 = mock(Comment.class);
            given(comment2.getId()).willReturn(2L);
            given(comment2.getContents()).willReturn("두 번째 댓글");
            given(comment2.getPostType()).willReturn(PostType.BOAST);
            given(comment2.getPostId()).willReturn(10L);
            given(comment2.getCreatedAt()).willReturn(now.minusHours(1));
            given(comment2.getUpdatedAt()).willReturn(now.minusHours(1));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Comment> commentPage = new PageImpl<>(List.of(comment1, comment2), pageable, 2);

            given(commentRepository.findByUserOrderByCreatedAtDesc(eq(user), eq(pageable)))
                    .willReturn(commentPage);

            // when
            PageResponse<MyCommentDto> response = myPageService.getMyComments(1L, pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(2L);
            assertThat(response.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 userId로 조회하면 CustomException(NOT_FOUND_USER)을 던진다")
        void test_실패_존재하지_않는_사용자_댓글_조회() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            Pageable pageable = PageRequest.of(0, 10);

            // when & then
            assertThatThrownBy(() -> myPageService.getMyComments(999L, pageable))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_USER);
        }

        @Test
        @DisplayName("성공: 댓글이 없는 경우 빈 목록과 totalElements=0을 반환한다")
        void test_성공_댓글_없는_경우_빈_목록_반환() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Comment> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(commentRepository.findByUserOrderByCreatedAtDesc(eq(user), eq(pageable)))
                    .willReturn(emptyPage);

            // when
            PageResponse<MyCommentDto> response = myPageService.getMyComments(1L, pageable);

            // then — 빈 목록
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0L);
        }
    }
}
