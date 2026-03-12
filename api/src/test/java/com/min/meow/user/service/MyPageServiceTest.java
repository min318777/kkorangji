package com.min.meow.user.service;

import com.min.meow.comment.entity.Comment;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.global.PostType;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import com.min.meow.user.dto.reponse.MyCommentListResponse;
import com.min.meow.user.dto.reponse.MyPageSummaryResponse;
import com.min.meow.user.dto.reponse.MyPostListResponse;
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

/**
 * MyPageService 유닛 테스트
 *
 * <h3>테스트 전략</h3>
 * <ul>
 *   <li>@ExtendWith(MockitoExtension): JUnit 5 + Mockito 통합, Spring 컨텍스트 불필요</li>
 *   <li>@InjectMocks: MyPageService의 모든 의존성을 Mock으로 주입</li>
 *   <li>DB 없이 순수 서비스 로직만 검증 — 빠르고 격리된 테스트</li>
 * </ul>
 *
 * <h3>FastAPI 매핑</h3>
 * pytest + unittest.mock.patch → @Mock + @InjectMocks
 * dependency_overrides[get_session] → Mock 객체 직접 반환값 설정
 */
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

    // =========================================================================
    // 테스트 픽스처 헬퍼 — 반복되는 Mock User 생성 공통화
    // =========================================================================

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

    // =========================================================================
    // getMyPageSummary 테스트
    // =========================================================================
    @Nested
    @DisplayName("getMyPageSummary — 마이페이지 요약 조회")
    class GetMyPageSummary {

        @Test
        @DisplayName("성공: 사용자가 존재하면 통계 포함 요약 정보를 반환한다")
        void test_성공_마이페이지_요약_조회() {
            // given — User Mock 설정
            User user = createTestUser(1L, "testuser");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(user));
            given(userRepository.countBoastCatPostsByUserId(any())).willReturn(10L);
            given(userRepository.countLostCatPostsByUserId(any())).willReturn(5L);
            given(commentRepository.countByUser(user)).willReturn(30L);

            // when
            MyPageSummaryResponse response = myPageService.getMyPageSummary("testuser");

            // then — totalPostCount = boastCount + lostCount = 10 + 5 = 15
            assertThat(response.getLoginId()).isEqualTo("testuser");
            assertThat(response.getBoastCatPostCount()).isEqualTo(10L);
            assertThat(response.getLostCatPostCount()).isEqualTo(5L);
            assertThat(response.getTotalPostCount()).isEqualTo(15L);
            assertThat(response.getTotalCommentCount()).isEqualTo(30L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 loginId로 조회하면 CustomException(UNREGISTERED_USER)을 던진다")
        void test_실패_존재하지_않는_사용자() {
            // given — 사용자 없음
            given(userRepository.findByLoginId("unknown")).willReturn(Optional.empty());

            // when & then — UNREGISTERED_USER 에러 발생
            assertThatThrownBy(() -> myPageService.getMyPageSummary("unknown"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.UNREGISTERED_USER);
        }

        @Test
        @DisplayName("성공: 게시글과 댓글이 없는 신규 사용자는 모든 카운트가 0이다")
        void test_성공_게시글_없는_신규_사용자() {
            // given — 모든 카운트 0
            User user = createTestUser(2L, "newuser");
            given(userRepository.findByLoginId("newuser")).willReturn(Optional.of(user));
            given(userRepository.countBoastCatPostsByUserId(any())).willReturn(0L);
            given(userRepository.countLostCatPostsByUserId(any())).willReturn(0L);
            given(commentRepository.countByUser(user)).willReturn(0L);

            // when
            MyPageSummaryResponse response = myPageService.getMyPageSummary("newuser");

            // then
            assertThat(response.getTotalPostCount()).isEqualTo(0L);
            assertThat(response.getBoastCatPostCount()).isEqualTo(0L);
            assertThat(response.getLostCatPostCount()).isEqualTo(0L);
            assertThat(response.getTotalCommentCount()).isEqualTo(0L);
        }
    }

    // =========================================================================
    // updateProfile 테스트
    // =========================================================================
    @Nested
    @DisplayName("updateProfile — 프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("성공: 닉네임 수정 시 user.updateNickname()이 호출된다")
        void test_성공_닉네임_수정() {
            // given
            User user = spy(createTestUser(1L, "testuser")); // spy로 메서드 호출 검증
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(user));
            given(userRepository.countBoastCatPostsByUserId(any())).willReturn(0L);
            given(userRepository.countLostCatPostsByUserId(any())).willReturn(0L);
            given(commentRepository.countByUser(user)).willReturn(0L);

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("새닉네임");

            // when
            myPageService.updateProfile("testuser", request);

            // then — updateNickname 도메인 메서드가 정확히 1회 호출되었는지 검증
            verify(user, times(1)).updateNickname("새닉네임");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 수정 시 CustomException(NOT_FOUND_USER)을 던진다")
        void test_실패_존재하지_않는_사용자_수정() {
            // given
            given(userRepository.findByLoginId("ghost")).willReturn(Optional.empty());

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("닉네임");

            // when & then — NOT_FOUND_USER: getMyPageSummary는 UNREGISTERED_USER이지만
            // updateProfile은 NOT_FOUND_USER로 다른 에러코드 사용
            assertThatThrownBy(() -> myPageService.updateProfile("ghost", request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_USER);
        }

        @Test
        @DisplayName("성공: 수정 후 반환된 응답에 새 닉네임이 반영된다")
        void test_성공_수정_후_응답에_새_닉네임_반영() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(user));
            given(userRepository.countBoastCatPostsByUserId(any())).willReturn(0L);
            given(userRepository.countLostCatPostsByUserId(any())).willReturn(0L);
            given(commentRepository.countByUser(user)).willReturn(0L);

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setNickname("새닉네임");

            // when — user.updateNickname() 실제 호출로 닉네임 변경
            MyPageSummaryResponse response = myPageService.updateProfile("testuser", request);

            // then — 변경된 닉네임이 응답에 반영
            assertThat(response.getNickname()).isEqualTo("새닉네임");
        }
    }

    // =========================================================================
    // getMyPosts 테스트
    // =========================================================================
    @Nested
    @DisplayName("getMyPosts — 내가 쓴 게시글 목록 조회")
    class GetMyPosts {

        @Test
        @DisplayName("성공: PostType.ALL 조회 시 자랑글과 실종글을 합산하여 반환한다")
        void test_성공_전체_게시글_조회() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(user));

            // BoastCatPost Mock — getAllPosts에서 createdAt으로 정렬하므로 null이 아닌 값 필요
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            BoastCatPost boastPost1 = mock(BoastCatPost.class);
            given(boastPost1.getId()).willReturn(1L);
            given(boastPost1.getTitle()).willReturn("자랑글1");
            given(boastPost1.getContents()).willReturn("내용1");
            given(boastPost1.getCreatedAt()).willReturn(now);
            given(boastPost1.getUpdatedAt()).willReturn(now);
            given(boastPost1.getCommentCount()).willReturn(0);
            given(boastPost1.getLikeCount()).willReturn(0);

            BoastCatPost boastPost2 = mock(BoastCatPost.class);
            given(boastPost2.getId()).willReturn(2L);
            given(boastPost2.getTitle()).willReturn("자랑글2");
            given(boastPost2.getContents()).willReturn("내용2");
            given(boastPost2.getCreatedAt()).willReturn(now.minusHours(1));
            given(boastPost2.getUpdatedAt()).willReturn(now.minusHours(1));
            given(boastPost2.getCommentCount()).willReturn(0);
            given(boastPost2.getLikeCount()).willReturn(0);

            LostCatPost lostPost = mock(LostCatPost.class);
            given(lostPost.getId()).willReturn(3L);
            given(lostPost.getTitle()).willReturn("실종글1");
            given(lostPost.getContents()).willReturn("내용3");
            given(lostPost.getCreatedAt()).willReturn(now.minusHours(2));
            given(lostPost.getUpdatedAt()).willReturn(now.minusHours(2));
            given(lostPost.getCommentCount()).willReturn(0);
            given(lostPost.isCompleted()).willReturn(false);

            // 자랑글 2개, 실종글 1개 반환 Mock 설정
            Page<BoastCatPost> boastPage = new PageImpl<>(
                    List.of(boastPost1, boastPost2), Pageable.unpaged(), 2
            );
            Page<LostCatPost> lostPage = new PageImpl<>(
                    List.of(lostPost), Pageable.unpaged(), 1
            );

            given(boastCatPostRepository.findByUserIdOrderByCreatedAtDesc(any(), eq(Pageable.unpaged())))
                    .willReturn(boastPage);
            given(lostCatRepository.findByUserIdOrderByCreatedAtDesc(any(), eq(Pageable.unpaged())))
                    .willReturn(lostPage);

            Pageable pageable = PageRequest.of(0, 10);

            // when
            MyPostListResponse response = myPageService.getMyPosts("testuser", pageable, PostType.ALL);

            // then — 자랑글 2 + 실종글 1 = 총 3개
            assertThat(response.getTotalElements()).isEqualTo(3L);

            // 두 repository가 모두 호출되었는지 검증
            verify(boastCatPostRepository).findByUserIdOrderByCreatedAtDesc(any(), eq(Pageable.unpaged()));
            verify(lostCatRepository).findByUserIdOrderByCreatedAtDesc(any(), eq(Pageable.unpaged()));
        }

        @Test
        @DisplayName("성공: PostType.BOAST 조회 시 BoastCatPostRepository만 호출된다")
        void test_성공_자랑글만_조회() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(user));

            Page<BoastCatPost> boastPage = new PageImpl<>(Collections.emptyList());
            Pageable pageable = PageRequest.of(0, 10);
            given(boastCatPostRepository.findByUserIdOrderByCreatedAtDesc(any(), eq(pageable)))
                    .willReturn(boastPage);

            // when
            myPageService.getMyPosts("testuser", pageable, PostType.BOAST);

            // then — boast만 조회, lost는 호출되지 않아야 함
            verify(boastCatPostRepository).findByUserIdOrderByCreatedAtDesc(any(), eq(pageable));
            verify(lostCatRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
        }

        @Test
        @DisplayName("성공: PostType.LOST 조회 시 LostCatRepository만 호출된다")
        void test_성공_실종글만_조회() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(user));

            Page<LostCatPost> lostPage = new PageImpl<>(Collections.emptyList());
            Pageable pageable = PageRequest.of(0, 10);
            given(lostCatRepository.findByUserIdOrderByCreatedAtDesc(any(), eq(pageable)))
                    .willReturn(lostPage);

            // when
            myPageService.getMyPosts("testuser", pageable, PostType.LOST);

            // then — lost만 조회, boast는 호출되지 않아야 함
            verify(lostCatRepository).findByUserIdOrderByCreatedAtDesc(any(), eq(pageable));
            verify(boastCatPostRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
        }
    }

    // =========================================================================
    // getMyComments 테스트
    // =========================================================================
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

            // MyCommentDto.from()에서 boastCatPost != null이면 boast로 분기
            // Comment Mock에 boastCatPost를 설정하여 NPE 방지
            BoastCatPost boastPost = mock(BoastCatPost.class);
            given(boastPost.getId()).willReturn(10L);
            given(boastPost.getTitle()).willReturn("자랑글 제목");

            Comment comment1 = mock(Comment.class);
            given(comment1.getId()).willReturn(1L);
            given(comment1.getContents()).willReturn("첫 번째 댓글");
            given(comment1.getBoastCatPost()).willReturn(boastPost);  // boast 댓글
            given(comment1.getCreatedAt()).willReturn(now);
            given(comment1.getUpdatedAt()).willReturn(now);

            Comment comment2 = mock(Comment.class);
            given(comment2.getId()).willReturn(2L);
            given(comment2.getContents()).willReturn("두 번째 댓글");
            given(comment2.getBoastCatPost()).willReturn(boastPost);  // boast 댓글
            given(comment2.getCreatedAt()).willReturn(now.minusHours(1));
            given(comment2.getUpdatedAt()).willReturn(now.minusHours(1));

            Pageable pageable = PageRequest.of(0, 10);
            Page<Comment> commentPage = new PageImpl<>(List.of(comment1, comment2), pageable, 2);

            given(commentRepository.findByUserOrderByCreatedAtDesc(eq(user), eq(pageable)))
                    .willReturn(commentPage);

            // when
            MyCommentListResponse response = myPageService.getMyComments(1L, pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(2L);
            assertThat(response.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 userId로 조회하면 CustomException(UNREGISTERED_USER)을 던진다")
        void test_실패_존재하지_않는_사용자_댓글_조회() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            Pageable pageable = PageRequest.of(0, 10);

            // when & then
            assertThatThrownBy(() -> myPageService.getMyComments(999L, pageable))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.UNREGISTERED_USER);
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
            MyCommentListResponse response = myPageService.getMyComments(1L, pageable);

            // then — 빈 목록
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0L);
        }
    }
}
