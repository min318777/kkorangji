package com.min.meow.user.controller;

import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.support.IntegrationTestBase;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MyPageController 통합 테스트")
class MyPageControllerTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Nested
    @DisplayName("GET /api/users/me — 마이페이지 요약 조회")
    class GetMyPageSummary {

        private User savedUser;

        @AfterEach
        void tearDown() {
            // 각 테스트 후 저장한 User 삭제 — 다른 테스트와 데이터 격리
            if (savedUser != null) {
                userRepository.deleteById(savedUser.getId());
            }
        }

        @Test
        @DisplayName("성공: DB에 저장된 사용자의 마이페이지를 조회하면 200과 loginId를 반환한다")
        void test_성공_마이페이지_요약_조회() {
            // given — DB에 User 저장 (실제 H2에 커밋됨)
            savedUser = createAndSaveUser(userRepository, "testuser", "testuser@test.com", "테스트냥이");

            // given — JWT 토큰 생성 (X-Auth-Version: v2로 DB 조회 없이 인증)
            HttpHeaders headers = createAuthHeader(savedUser.getId(), "ROLE_USER");

            // when — 실제 HTTP GET 요청
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            // then — 200 OK
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("success")).isEqualTo(true);
            assertThat(body.get("message")).isEqualTo("마이페이지 조회 성공");

            // 응답 데이터 검증
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            assertThat(data.get("loginId")).isEqualTo("testuser");
            assertThat(data.get("nickname")).isEqualTo("테스트냥이");

            // password 필드가 응답에 노출되지 않아야 함 (보안 검증)
            assertThat(data.containsKey("password")).isFalse();
        }

        @Test
        @DisplayName("실패: 인증 토큰 없이 접근하면 4xx를 반환한다")
        void test_실패_인증_없이_조회() {
            // given — 헤더 없이 요청 (비인증 상태)
            // SecurityConfig에 authenticationEntryPoint 미설정 → 기본 403 응답
            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    Map.class
            );

            // then — 4xx 에러 (인증 없는 접근 거부)
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }

        @Test
        @DisplayName("성공: 신규 사용자의 통계는 모두 0이다")
        void test_성공_게시글_댓글_통계_포함_조회() {
            // given — 신규 User 저장 (게시글/댓글 없음)
            savedUser = createAndSaveUser(userRepository, "newstatuser", "newstatuser@test.com", "신규사용자");
            HttpHeaders headers = createAuthHeader(savedUser.getId(), "ROLE_USER");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");

            // 신규 사용자 — 게시글/댓글 없음 → 모두 0
            assertThat(data.get("totalCommentCount")).isEqualTo(0);
            assertThat(data.get("totalPostCount")).isEqualTo(0);
            assertThat(data.get("boastCatPostCount")).isEqualTo(0);
            assertThat(data.get("lostCatPostCount")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me — 프로필 수정")
    class UpdateProfile {

        private User savedUser;

        @AfterEach
        void tearDown() {
            if (savedUser != null) {
                userRepository.deleteById(savedUser.getId());
            }
        }

        @Test
        @DisplayName("성공: 유효한 닉네임으로 수정하면 200과 변경된 닉네임을 반환한다")
        void test_성공_닉네임_수정() {
            // given — User 저장
            savedUser = createAndSaveUser(userRepository, "patchuser", "patchuser@test.com", "기존닉네임");

            HttpHeaders headers = createAuthHeader(savedUser.getId(), "ROLE_USER");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Map.of("nickname", "홍길동");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me",
                    HttpMethod.PATCH,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 200 OK + 변경된 닉네임 반환
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("nickname")).isEqualTo("홍길동");

            // DB에 실제로 반영되었는지 검증 (통합 테스트의 핵심)
            User updatedUser = userRepository.findByLoginId("patchuser").orElseThrow();
            assertThat(updatedUser.getNickname()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("실패: 인증 토큰 없이 수정 요청하면 4xx를 반환한다")
        void test_실패_인증_없이_수정() {
            // given — 헤더 없음
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Map.of("nickname", "홍길동");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me",
                    HttpMethod.PATCH,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 4xx 에러
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }

        @Test
        @DisplayName("실패: 닉네임이 빈값이면 400 Bad Request를 반환한다 (@NotBlank 검증)")
        void test_실패_닉네임_빈값() {
            // given
            savedUser = createAndSaveUser(userRepository, "blankuser", "blankuser@test.com", "기존닉네임");

            HttpHeaders headers = createAuthHeader(savedUser.getId(), "ROLE_USER");
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 빈 문자열 — @NotBlank 위반
            Map<String, String> requestBody = Map.of("nickname", "");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me",
                    HttpMethod.PATCH,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("실패: 닉네임에 숫자가 포함되면 400 Bad Request를 반환한다 (@Pattern 검증)")
        void test_실패_닉네임_숫자포함() {
            // given
            savedUser = createAndSaveUser(userRepository, "patternuser", "patternuser@test.com", "기존닉네임");

            HttpHeaders headers = createAuthHeader(savedUser.getId(), "ROLE_USER");
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 숫자 포함 — @Pattern(^[가-힣a-zA-Z]+$) 위반
            Map<String, String> requestBody = Map.of("nickname", "홍길1");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me",
                    HttpMethod.PATCH,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/posts — 내가 쓴 글 목록 조회")
    class GetMyPosts {

        private User savedUser;

        @AfterEach
        void tearDown() {
            if (savedUser != null) {
                userRepository.deleteById(savedUser.getId());
            }
        }

        @Test
        @DisplayName("성공: 게시글 없는 사용자가 조회하면 200과 빈 content 배열을 반환한다")
        void test_성공_내가_쓴_글_목록_조회() {
            // given
            savedUser = createAndSaveUser(userRepository, "postuser", "postuser@test.com", "게시글러");
            HttpHeaders headers = createAuthHeader(savedUser.getId(), "ROLE_USER");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me/posts?page=0&size=10&type=ALL",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            // then — 200 OK + 페이징 구조 검증
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = response.getBody();
            assertThat(body.get("success")).isEqualTo(true);
            assertThat(body.get("message")).isEqualTo("내가 쓴 글 조회 성공");

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            // content는 배열(List) 타입이어야 함
            assertThat(data.get("content")).isInstanceOf(java.util.List.class);
            assertThat(data.get("totalElements")).isEqualTo(0);
            assertThat(data.get("currentPage")).isEqualTo(0);
            assertThat(data.get("size")).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/comments — 내가 쓴 댓글 목록 조회")
    class GetMyComments {

        private User savedUser;

        @AfterEach
        void tearDown() {
            if (savedUser != null) {
                userRepository.deleteById(savedUser.getId());
            }
        }

        @Test
        @DisplayName("성공: 댓글 없는 사용자가 조회하면 200과 totalElements=0을 반환한다")
        void test_성공_내가_쓴_댓글_목록_조회() {
            // given
            savedUser = createAndSaveUser(userRepository, "commentowner", "commentowner@test.com", "댓글주인");
            HttpHeaders headers = createAuthHeader(savedUser.getId(), "ROLE_USER");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me/comments?page=0&size=10",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            // then — 200 OK + totalElements 정확성 검증
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            assertThat(data.get("totalElements")).isEqualTo(0);
            assertThat(data.get("content")).isInstanceOf(java.util.List.class);
        }
    }

    @Nested
    @DisplayName("인증 토큰 불일치 — DB에 없는 loginId")
    class TokenMismatch {

        @Test
        @DisplayName("실패: JWT에 DB에 없는 loginId가 담겨있으면 4xx 에러를 반환한다")
        void test_실패_다른_사용자_loginId_조회_불가() {
            // given — DB에 저장하지 않은 사용자의 loginId로 토큰 생성
            // v2 인증: JwtAuthenticationFilter는 통과하지만 MyPageService.getMyPageSummary에서
            // userRepository.findByLoginId("nonexistent_user") → empty → UNREGISTERED_USER 예외
            HttpHeaders headers = createAuthHeader(99999L, "ROLE_USER");

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            // then — UNREGISTERED_USER는 HttpStatus.UNAUTHORIZED(401) 또는 그 외 4xx
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }
}
