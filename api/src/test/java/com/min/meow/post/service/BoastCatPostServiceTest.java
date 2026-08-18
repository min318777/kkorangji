package com.min.meow.post.service;

import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.common.PostType;
import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.config.S3Service;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.ImageItemRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoastCatPostService 유닛 테스트")
class BoastCatPostServiceTest {

    @InjectMocks
    private BoastCatPostService boastCatPostService;

    @Mock
    private BoastCatPostRepository boastCatPostRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private BoastCatPostCountCacheService countCacheService;

    @Mock
    private ViewCountService viewCountService;

    @Mock
    private PopularRankingService popularRankingService;

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

    private BoastCatPost createPost(Long id, User writer) {
        return BoastCatPost.builder()
                .id(id)
                .title("원래 제목")
                .contents("원래 내용")
                .user(writer)
                .imageUrls(new ArrayList<>(List.of("https://cdn.example.com/old.jpg")))
                .build();
    }

    @Nested
    @DisplayName("createBoastCatPost() — 글 작성")
    class Create {

        @Test
        @DisplayName("성공: 이미지 key를 CloudFront URL로 변환하고 첫 이미지를 썸네일로 저장한다")
        void test_성공_이미지_포함_작성() {
            // given
            Long userId = 1L;
            User writer = createUser(userId);
            CreateBoastCatPostRequest request = CreateBoastCatPostRequest.builder()
                    .title("자랑글 제목")
                    .content("내용")
                    .imageKeys(List.of("meow/a.jpg", "meow/b.jpg"))
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(s3Service.toCloudFrontUrls(request.getImageKeys()))
                    .willReturn(List.of("https://cdn.example.com/a.jpg", "https://cdn.example.com/b.jpg"));
            given(boastCatPostRepository.save(any(BoastCatPost.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            CreateBoastCatPostResponse response = boastCatPostService.createBoastCatPost(request, userId);

            // then
            assertThat(response.getImageUrls()).containsExactly(
                    "https://cdn.example.com/a.jpg", "https://cdn.example.com/b.jpg");
            then(countCacheService).should().evict();
        }

        @Test
        @DisplayName("성공: 이미지 없이 작성해도 정상 처리된다 (자랑글은 사진 없이도 작성 가능)")
        void test_성공_이미지_없이_작성() {
            // given
            Long userId = 1L;
            User writer = createUser(userId);
            CreateBoastCatPostRequest request = CreateBoastCatPostRequest.builder()
                    .title("자랑글 제목")
                    .content("내용")
                    .imageKeys(null)
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(boastCatPostRepository.save(any(BoastCatPost.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            CreateBoastCatPostResponse response = boastCatPostService.createBoastCatPost(request, userId);

            // then
            assertThat(response.getImageUrls()).isEmpty();
            then(s3Service).should(never()).toCloudFrontUrls(anyList());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자면 UNAUTHORIZED 예외를 던진다")
        void test_실패_존재하지_않는_사용자() {
            // given
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());
            CreateBoastCatPostRequest request = CreateBoastCatPostRequest.builder().title("제목").build();

            // when & then
            assertThatThrownBy(() -> boastCatPostService.createBoastCatPost(request, userId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("updateBoastCatPost() — 글 수정")
    class Update {

        @Test
        @DisplayName("성공: 본인 글이면 수정할 수 있다")
        void test_성공_본인_글_수정() {
            // given
            Long userId = 1L;
            Long postId = 10L;
            User writer = createUser(userId);
            BoastCatPost post = createPost(postId, writer);
            UpdateBoastCatPostRequest request = UpdateBoastCatPostRequest.builder()
                    .title("수정된 제목")
                    .content("수정된 내용")
                    .images(List.of())
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            UpdateBoastCatPostResponse response = boastCatPostService.updateBoastCatPost(request, postId, userId);

            // then
            assertThat(response.getTitle()).isEqualTo("수정된 제목");
            // 기존 이미지가 최종 목록에서 빠졌으므로 S3에서 삭제되어야 함
            then(s3Service).should().deleteFiles(anyList());
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
            BoastCatPost post = createPost(postId, writer);
            UpdateBoastCatPostRequest request = UpdateBoastCatPostRequest.builder().title("수정 시도").build();

            given(userRepository.findById(otherUserId)).willReturn(Optional.of(other));
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then — 수정은 post:delete 권한과 무관하게 본인만 가능
            assertThatThrownBy(() -> boastCatPostService.updateBoastCatPost(request, postId, otherUserId))
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
            BoastCatPost post = createPost(postId, writer);
            UpdateBoastCatPostRequest request = UpdateBoastCatPostRequest.builder()
                    .title("제목")
                    .images(List.of(ImageItemRequest.builder()
                            .type(ImageItemRequest.ImageType.NEW)
                            .value("meow/new.jpg")
                            .build()))
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));
            given(s3Service.toCloudFrontUrl("meow/new.jpg")).willReturn("https://cdn.example.com/new.jpg");

            // when
            UpdateBoastCatPostResponse response = boastCatPostService.updateBoastCatPost(request, postId, userId);

            // then
            assertThat(response.getImageUrls()).containsExactly("https://cdn.example.com/new.jpg");
        }
    }

    @Nested
    @DisplayName("deleteBoastCatPost() — 글 삭제")
    class Delete {

        @Test
        @DisplayName("성공: 본인 글은 삭제할 수 있고 인기글 랭킹에서도 함께 제거된다")
        void test_성공_본인_글_삭제() {
            // given
            Long userId = 1L;
            Long postId = 10L;
            User writer = createUser(userId);
            BoastCatPost post = createPost(postId, writer);

            given(userRepository.findById(userId)).willReturn(Optional.of(writer));
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            boastCatPostService.deleteBoastCatPost(postId, userId, false);

            // then
            then(commentRepository).should().deleteAllByPostIdAndPostType(postId, PostType.BOAST);
            then(boastCatPostRepository).should().deleteById(postId);
            then(popularRankingService).should().removeFromRanking(postId);
        }

        @Test
        @DisplayName("성공: 작성자가 아니어도 post:delete 권한이 있으면 삭제할 수 있다")
        void test_성공_관리자_권한으로_타인_글_삭제() {
            // given
            Long writerId = 1L;
            Long adminId = 2L;
            Long postId = 10L;
            User writer = createUser(writerId);
            User admin = createUser(adminId);
            BoastCatPost post = createPost(postId, writer);

            given(userRepository.findById(adminId)).willReturn(Optional.of(admin));
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            boastCatPostService.deleteBoastCatPost(postId, adminId, true);

            // then
            then(boastCatPostRepository).should().deleteById(postId);
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
            BoastCatPost post = createPost(postId, writer);

            given(userRepository.findById(otherUserId)).willReturn(Optional.of(other));
            given(boastCatPostRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> boastCatPostService.deleteBoastCatPost(postId, otherUserId, false))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN_NOT_AUTHOR);

            then(boastCatPostRepository).should(never()).deleteById(any());
            then(popularRankingService).should(never()).removeFromRanking(any());
        }
    }
}
