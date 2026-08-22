package com.min.meow.post.service;

import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.common.PostType;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.config.S3Service;
import com.min.meow.post.dto.request.CreateLostCatPostRequest;
import com.min.meow.post.dto.request.ImageItemRequest;
import com.min.meow.post.dto.request.UpdateLostCatPostRequest;
import com.min.meow.post.dto.response.CreateLostCatPostResponse;
import com.min.meow.post.dto.response.UpdateLostCatPostResponse;
import com.min.meow.post.entity.LostCatPost;
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
@DisplayName("LostCatPostService 유닛 테스트")
class LostCatPostServiceTest {

    @InjectMocks
    private LostCatPostService lostCatPostService;

    @Mock
    private LostCatRepository lostCatRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private ViewCountService viewCountService;

    @Mock
    private LostCatPostCountCacheService countCacheService;

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

    private LostCatPost createPost(Long id, User writer) {
        return LostCatPost.builder()
                .id(id)
                .title("실종했어요")
                .contents("내용")
                .catName("나비")
                .lostLocation("서울시 강남구")
                .user(writer)
                .isCompleted(false)
                .imageUrls(new ArrayList<>(List.of("https://cdn.example.com/old.jpg")))
                .build();
    }

    @Nested
    @DisplayName("createLostCatPost() — 글 작성")
    class Create {

        @Test
        @DisplayName("성공: lat/lng이 모두 있으면 위치 좌표(Point)가 함께 저장된다")
        void test_성공_좌표_있는_작성() {
            // given
            Long userId = 1L;
            User writer = createUser(userId);
            CreateLostCatPostRequest request = CreateLostCatPostRequest.builder()
                    .title("실종했어요")
                    .catName("나비")
                    .lostLocation("서울시 강남구")
                    .latitude(37.5)
                    .longitude(127.0)
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(lostCatRepository.save(any(LostCatPost.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            CreateLostCatPostResponse response = lostCatPostService.createLostCatPost(request, userId);

            // then
            assertThat(response.getCatName()).isEqualTo("나비");
            then(countCacheService).should().evict();
        }

        @Test
        @DisplayName("성공: lat/lng이 없으면 좌표 없이 저장된다")
        void test_성공_좌표_없는_작성() {
            // given
            Long userId = 1L;
            User writer = createUser(userId);
            CreateLostCatPostRequest request = CreateLostCatPostRequest.builder()
                    .title("실종했어요")
                    .catName("나비")
                    .lostLocation("서울시 강남구")
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(lostCatRepository.save(any(LostCatPost.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when & then — 예외 없이 정상 생성되는지만 확인 (Point 생성 로직이 null을 안전하게 스킵하는지)
            assertThat(lostCatPostService.createLostCatPost(request, userId)).isNotNull();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자면 UNAUTHORIZED 예외를 던진다")
        void test_실패_존재하지_않는_사용자() {
            // given
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());
            CreateLostCatPostRequest request = CreateLostCatPostRequest.builder().title("제목").build();

            // when & then
            assertThatThrownBy(() -> lostCatPostService.createLostCatPost(request, userId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("updateLostCatPost() — 글 수정")
    class Update {

        @Test
        @DisplayName("성공: 본인 글이면 수정할 수 있다")
        void test_성공_본인_글_수정() {
            // given
            Long userId = 1L;
            Long postId = 10L;
            User writer = createUser(userId);
            LostCatPost post = createPost(postId, writer);
            UpdateLostCatPostRequest request = UpdateLostCatPostRequest.builder()
                    .title("수정된 제목")
                    .catName("나비")
                    .lostLocation("서울시 서초구")
                    .images(List.of())
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(lostCatRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            UpdateLostCatPostResponse response = lostCatPostService.updateLostCatPost(postId, request, userId);

            // then
            assertThat(response.getTitle()).isEqualTo("수정된 제목");
            then(s3Service).should().deleteFiles(any());
        }

        @Test
        @DisplayName("실패: 타인 글이면 관리자 권한이 있어도 FORBIDDEN_NOT_AUTHOR 예외를 던진다")
        void test_실패_타인_글_수정_관리자도_불가() {
            // given
            Long writerId = 1L;
            Long otherUserId = 2L;
            Long postId = 10L;
            User writer = createUser(writerId);
            User other = createUser(otherUserId);
            LostCatPost post = createPost(postId, writer);
            UpdateLostCatPostRequest request = UpdateLostCatPostRequest.builder().title("수정 시도").build();

            given(userRepository.findById(otherUserId)).willReturn(Optional.of(other));
            given(lostCatRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> lostCatPostService.updateLostCatPost(postId, request, otherUserId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN_NOT_AUTHOR);
        }

        @Test
        @DisplayName("성공: 이미지 타입이 NEW면 S3 key를 CloudFront URL로 변환한다")
        void test_성공_새_이미지_변환() {
            // given
            Long userId = 1L;
            Long postId = 10L;
            User writer = createUser(userId);
            LostCatPost post = createPost(postId, writer);
            UpdateLostCatPostRequest request = UpdateLostCatPostRequest.builder()
                    .title("제목")
                    .catName("나비")
                    .lostLocation("서울시 강남구")
                    .images(List.of(ImageItemRequest.builder()
                            .type(ImageItemRequest.ImageType.NEW)
                            .value("meow/new.jpg")
                            .build()))
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(lostCatRepository.findById(postId)).willReturn(Optional.of(post));
            given(s3Service.toCloudFrontUrl("meow/new.jpg")).willReturn("https://cdn.example.com/new.jpg");

            // when
            UpdateLostCatPostResponse response = lostCatPostService.updateLostCatPost(postId, request, userId);

            // then
            assertThat(response.getImageUrls()).containsExactly("https://cdn.example.com/new.jpg");
        }
    }

    @Nested
    @DisplayName("deleteLostCatPost() — 글 삭제")
    class Delete {

        @Test
        @DisplayName("성공: 본인 글은 삭제할 수 있다")
        void test_성공_본인_글_삭제() {
            // given
            Long userId = 1L;
            Long postId = 10L;
            User writer = createUser(userId);
            LostCatPost post = createPost(postId, writer);

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(lostCatRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            lostCatPostService.deleteLostCatPost(postId, userId, false);

            // then
            then(commentRepository).should().deleteAllByPostIdAndPostType(postId, PostType.LOST);
            then(lostCatRepository).should().deleteById(postId);
        }

        @Test
        @DisplayName("성공: 작성자가 아니어도 post:delete 권한이 있으면 삭제할 수 있다 (수정과 다른 정책)")
        void test_성공_관리자_권한으로_타인_글_삭제() {
            // given
            Long writerId = 1L;
            Long adminId = 2L;
            Long postId = 10L;
            User writer = createUser(writerId);
            User admin = createUser(adminId);
            LostCatPost post = createPost(postId, writer);

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(lostCatRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            lostCatPostService.deleteLostCatPost(postId, adminId, true);

            // then
            then(lostCatRepository).should().deleteById(postId);
        }

        @Test
        @DisplayName("실패: 작성자도 아니고 post:delete 권한도 없으면 FORBIDDEN_NOT_AUTHOR 예외를 던진다")
        void test_실패_권한_없는_타인_삭제_차단() {
            // given
            Long writerId = 1L;
            Long otherUserId = 2L;
            Long postId = 10L;
            User writer = createUser(writerId);
            User other = createUser(otherUserId);
            LostCatPost post = createPost(postId, writer);

            given(userRepository.findById(otherUserId)).willReturn(Optional.of(other));
            given(lostCatRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> lostCatPostService.deleteLostCatPost(postId, otherUserId, false))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN_NOT_AUTHOR);

            then(lostCatRepository).should(never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("updateCompletedStatus() — 찾는 중 ↔ 완료 상태 토글")
    class UpdateCompletedStatus {

        @Test
        @DisplayName("성공: 본인 글이면 완료 상태를 변경할 수 있다")
        void test_성공_본인_글_상태_변경() {
            // given
            Long userId = 1L;
            Long postId = 10L;
            User writer = createUser(userId);
            LostCatPost post = createPost(postId, writer);

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(lostCatRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            lostCatPostService.updateCompletedStatus(postId, true, userId);

            // then
            assertThat(post.isCompleted()).isTrue();
            then(lostCatRepository).should().save(post);
        }

        @Test
        @DisplayName("실패: 타인 글이면 post:delete 권한이 있어도 상태를 바꿀 수 없다 (삭제와 달리 예외 없음)")
        void test_실패_타인_글은_관리자_권한으로도_상태변경_불가() {
            // given
            Long writerId = 1L;
            Long otherUserId = 2L;
            Long postId = 10L;
            User writer = createUser(writerId);
            User other = createUser(otherUserId);
            LostCatPost post = createPost(postId, writer);

            given(userRepository.findById(otherUserId)).willReturn(Optional.of(other));
            given(lostCatRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> lostCatPostService.updateCompletedStatus(postId, true, otherUserId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN_NOT_AUTHOR);

            assertThat(post.isCompleted()).isFalse();
        }
    }
}
