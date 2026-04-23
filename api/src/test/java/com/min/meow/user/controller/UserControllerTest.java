package com.min.meow.user.controller;

import com.min.meow.support.IntegrationTestBase;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserController 통합 테스트 — 회원가입 & 로그인
 *
 * <h3>FastAPI 매핑</h3>
 * <pre>
 * FastAPI (test_auth.py)                Java (UserControllerTest)
 * ─────────────────────────────────     ──────────────────────────────────────
 * class TestRegister                →   @Nested class Join
 * class TestLogin                   →   @Nested class Login
 * unauthenticated_client            →   restTemplate (토큰 없이 요청)
 * test_session.add(user)            →   createAndSaveUser() + userRepository.save()
 * assert response.status_code == X  →   assertThat(response.getStatusCode()).isEqualTo(X)
 * assert "password" not in data     →   assertThat(data.containsKey("password")).isFalse()
 * </pre>
 *
 * <h3>테스트 대상 엔드포인트</h3>
 * <ul>
 *   <li>POST /api/users/join  — 회원가입 (인증 불필요)</li>
 *   <li>POST /login           — 로그인 (Spring Security CustomLoginFilter 처리)</li>
 * </ul>
 *
 * <h3>로그인 보안 원칙</h3>
 * "아이디 없음"과 "비밀번호 틀림"을 모두 401로 통일합니다.
 * 두 경우를 구분하면 공격자가 계정 존재 여부를 알 수 있기 때문입니다.
 * (User Enumeration Attack 방지)
 */
@DisplayName("UserController 통합 테스트 — 회원가입 & 로그인")
class UserControllerTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    // =========================================================================
    // POST /api/users/join — 회원가입
    // FastAPI: class TestRegister
    // =========================================================================
    @Nested
    @DisplayName("POST /api/users/join — 회원가입")
    class Join {

        // 회원가입 테스트에서 생성된 User 추적 (tearDown에서 삭제)
        private String createdLoginId;

        @AfterEach
        void tearDown() {
            // 각 테스트 후 생성된 User 삭제 — 테스트 간 데이터 격리
            if (createdLoginId != null) {
                userRepository.findByLoginId(createdLoginId)
                        .ifPresent(u -> userRepository.deleteById(u.getId()));
            }
        }

        @Test
        @DisplayName("성공: 유효한 정보로 회원가입하면 201과 사용자 정보를 반환한다")
        void test_성공_회원가입() {
            // FastAPI: test_성공_회원가입
            // given — 유효한 회원가입 요청 바디
            createdLoginId = "newcat01";
            Map<String, String> requestBody = Map.of(
                    "loginId", createdLoginId,
                    "password", "password1!",
                    "passwordConfirm", "password1!",
                    "email", "newcat@example.com",
                    "nickname", "냥이"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/join",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 201 Created
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("success")).isEqualTo(true);
            assertThat(body.get("message")).isEqualTo("회원가입 성공");

            // 응답 data 검증
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            assertThat(data.get("loginId")).isEqualTo("newcat01");
            assertThat(data.get("nickname")).isEqualTo("냥이");
            assertThat(data.get("email")).isEqualTo("newcat@example.com");

            // 비밀번호가 응답에 노출되지 않아야 함 (보안 핵심)
            // FastAPI: assert "password" not in data / assert "hashed_password" not in data
            assertThat(data.containsKey("password")).isFalse();
            assertThat(data.containsKey("hashed_password")).isFalse();

            // 역할이 ROLE_USER로 부여되어야 함
            assertThat(data.get("roles")).isNotNull();
        }

        @Test
        @DisplayName("실패: 이미 존재하는 loginId로 가입하면 409 Conflict를 반환한다")
        void test_실패_아이디_중복() {
            // FastAPI: test_실패_이메일_중복 (우리 프로젝트는 loginId + email 모두 유니크 검증)
            // given — DB에 동일 loginId를 가진 User 저장
            createdLoginId = "dupcat01";
            createAndSaveUser(userRepository, createdLoginId, "original@example.com", "원본냥이");

            Map<String, String> requestBody = Map.of(
                    "loginId", createdLoginId,        // 중복 loginId
                    "password", "password1!",
                    "passwordConfirm", "password1!",
                    "email", "another@example.com",   // 다른 이메일이라도
                    "nickname", "다른냥이"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/join",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 409 Conflict (ALREADY_EXISTING_USER)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("실패: 이미 존재하는 이메일로 가입하면 409 Conflict를 반환한다")
        void test_실패_이메일_중복() {
            // given — DB에 동일 email을 가진 User 저장
            createdLoginId = "emaildup01";
            createAndSaveUser(userRepository, "anothercat", "dup@example.com", "이메일중복냥이");

            Map<String, String> requestBody = Map.of(
                    "loginId", createdLoginId,        // 다른 loginId라도
                    "password", "password1!",
                    "passwordConfirm", "password1!",
                    "email", "dup@example.com",       // 중복 이메일
                    "nickname", "새냥이"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/join",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 409 Conflict (ALREADY_EXISTING_EMAIL)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            // tearDown에서 생성된 emaildup01은 없으므로 anothercat을 직접 삭제
            userRepository.findByLoginId("anothercat")
                    .ifPresent(u -> userRepository.deleteById(u.getId()));
            createdLoginId = null; // tearDown에서 중복 삭제 방지
        }

        @Test
        @DisplayName("실패: 비밀번호가 숫자만 포함하면 (영문 없음) 400 Bad Request를 반환한다 (@Pattern 검증)")
        void test_실패_비밀번호_형식_불충족() {
            // FastAPI: test_실패_비밀번호_너무_짧음 → 우리 프로젝트는 형식(영문+숫자 필수) 검증
            // given — 숫자만 있는 비밀번호 (영문 없음)
            Map<String, String> requestBody = Map.of(
                    "loginId", "validcat1",
                    "password", "12345678",     // 숫자만 — @Pattern 위반 (영문 필수)
                    "passwordConfirm", "12345678",
                    "email", "valid@example.com",
                    "nickname", "유효냥이"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/join",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 400 Bad Request (@Valid 실패 → GlobalExceptionHandler 처리)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("실패: 비밀번호와 비밀번호 확인이 다르면 400 Bad Request를 반환한다")
        void test_실패_비밀번호_불일치() {
            // given — password != passwordConfirm
            Map<String, String> requestBody = Map.of(
                    "loginId", "mismatch1",
                    "password", "password1!",
                    "passwordConfirm", "different1!",   // 불일치
                    "email", "mismatch@example.com",
                    "nickname", "불일치냥이"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/join",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 400 Bad Request (@AssertTrue(isPasswordMatching) 위반)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("실패: 닉네임에 숫자가 포함되면 400 Bad Request를 반환한다")
        void test_실패_닉네임_숫자_포함() {
            // given — 숫자 포함 닉네임 (@Pattern(^[가-힣a-zA-Z]+$) 위반)
            Map<String, String> requestBody = Map.of(
                    "loginId", "numcat01",
                    "password", "password1!",
                    "passwordConfirm", "password1!",
                    "email", "numcat@example.com",
                    "nickname", "냥이1"             // 숫자 포함 — 위반
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/join",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("실패: loginId가 4자 이하면 400 Bad Request를 반환한다 (@Size 검증)")
        void test_실패_아이디_너무_짧음() {
            // given — 4자 loginId (@Size(min=5) 위반)
            Map<String, String> requestBody = Map.of(
                    "loginId", "cat",           // 3자 — 위반
                    "password", "password1!",
                    "passwordConfirm", "password1!",
                    "email", "short@example.com",
                    "nickname", "짧은냥이"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/users/join",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // then — 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // =========================================================================
    // POST /login — 로그인 (Spring Security CustomLoginFilter 처리)
    // FastAPI: class TestLogin
    // =========================================================================
    @Nested
    @DisplayName("POST /login — 로그인")
    class Login {

        private User savedUser;

        @AfterEach
        void tearDown() {
            // 각 테스트 후 저장한 User 삭제
            if (savedUser != null) {
                userRepository.deleteById(savedUser.getId());
            }
        }

        @Test
        @DisplayName("성공: 올바른 자격증명으로 로그인하면 200과 JWT 토큰을 반환한다")
        void test_성공_로그인() {
            // FastAPI: test_성공_로그인
            // given — DB에 User 저장 (BCrypt 인코딩된 비밀번호 필요)
            // createAndSaveUser는 "encoded_password"를 평문으로 저장하므로
            // 로그인 테스트는 회원가입 API를 통해 User를 생성한 후 로그인한다
            HttpHeaders joinHeaders = new HttpHeaders();
            joinHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> joinBody = Map.of(
                    "loginId", "logintest1",
                    "password", "password1!",
                    "passwordConfirm", "password1!",
                    "email", "logintest@example.com",
                    "nickname", "로그인냥이"
            );

            // 회원가입으로 BCrypt 인코딩된 User 생성
            restTemplate.exchange(
                    "/api/users/join",
                    HttpMethod.POST,
                    new HttpEntity<>(joinBody, joinHeaders),
                    Map.class
            );
            savedUser = userRepository.findByLoginId("logintest1").orElseThrow();

            // given — 로그인 요청
            HttpHeaders loginHeaders = new HttpHeaders();
            loginHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> loginBody = Map.of(
                    "loginId", "logintest1",
                    "password", "password1!"    // 올바른 비밀번호
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/login",
                    HttpMethod.POST,
                    new HttpEntity<>(loginBody, loginHeaders),
                    Map.class
            );

            // then — 200 OK
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();

            // JWT Access Token이 응답에 포함되어야 함
            // FastAPI: assert "access_token" in data
            assertThat(body.get("accessToken")).isNotNull();
            assertThat(body.get("success")).isEqualTo(true);

            // Authorization 헤더에도 토큰이 설정되어야 함
            assertThat(response.getHeaders().getFirst("Authorization")).startsWith("Bearer ");
        }

        @Test
        @DisplayName("실패: 잘못된 비밀번호로 로그인하면 401 Unauthorized를 반환한다")
        void test_실패_잘못된_비밀번호() {
            // FastAPI: test_실패_잘못된_비밀번호
            // 보안 원칙: "비밀번호가 틀렸다"는 정보도 공격자에게 유용하므로
            // 아이디 없음과 동일한 401을 반환한다 (User Enumeration Attack 방지)

            // given — 회원가입 후 User 생성
            HttpHeaders joinHeaders = new HttpHeaders();
            joinHeaders.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.exchange(
                    "/api/users/join",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of(
                            "loginId", "wrongpw01",
                            "password", "password1!",
                            "passwordConfirm", "password1!",
                            "email", "wrongpw@example.com",
                            "nickname", "비밀번호냥이"
                    ), joinHeaders),
                    Map.class
            );
            savedUser = userRepository.findByLoginId("wrongpw01").orElseThrow();

            // given — 잘못된 비밀번호로 로그인 시도
            HttpHeaders loginHeaders = new HttpHeaders();
            loginHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> loginBody = Map.of(
                    "loginId", "wrongpw01",
                    "password", "wrongpassword1!"   // 틀린 비밀번호
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/login",
                    HttpMethod.POST,
                    new HttpEntity<>(loginBody, loginHeaders),
                    Map.class
            );

            // then — 401 Unauthorized (CustomLoginFilter.unsuccessfulAuthentication)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 loginId로 로그인하면 401 Unauthorized를 반환한다")
        void test_실패_존재하지_않는_아이디() {
            // FastAPI: test_실패_존재하지_않는_이메일
            // 핵심: 비밀번호 틀림(위)과 동일한 401 반환 → 계정 존재 여부를 알 수 없음

            // given — DB에 없는 loginId
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> loginBody = Map.of(
                    "loginId", "nonexist1",     // DB에 없는 아이디
                    "password", "anypassword1!"
            );

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/login",
                    HttpMethod.POST,
                    new HttpEntity<>(loginBody, headers),
                    Map.class
            );

            // then — 401 Unauthorized
            // 비밀번호 틀림(test_실패_잘못된_비밀번호)과 동일한 응답 코드 → 계정 존재 여부 비공개
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // =========================================================================
    // DELETE /api/users/withdraw — 회원 탈퇴 (인증 필요)
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/users/withdraw — 회원 탈퇴")
    class Withdraw {

        private User savedUser;

        @AfterEach
        void tearDown() {
            if (savedUser != null) {
                userRepository.findById(savedUser.getId())
                        .ifPresent(u -> userRepository.deleteById(u.getId()));
            }
        }

        @Test
        @DisplayName("성공: 인증된 사용자가 탈퇴하면 204 No Content를 반환한다")
        void test_성공_회원_탈퇴() {
            // given — User 생성
            savedUser = createAndSaveUser(userRepository, "withdraw1", "withdraw@test.com", "탈퇴냥이");
            HttpHeaders headers = createAuthHeader(savedUser.getId(), "ROLE_USER");

            // when
            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/users/withdraw",
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    Void.class
            );

            // then — 204 No Content
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // DB에서 소프트 삭제 확인 (isDelete = true)
            User withdrawnUser = userRepository.findById(savedUser.getId()).orElseThrow();
            assertThat(withdrawnUser.isDelete()).isTrue();
        }

        @Test
        @DisplayName("실패: 인증 없이 탈퇴 요청하면 4xx를 반환한다")
        void test_실패_인증_없이_탈퇴() {
            // given — 헤더 없음

            // when
            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/users/withdraw",
                    HttpMethod.DELETE,
                    HttpEntity.EMPTY,
                    Void.class
            );

            // then — 4xx (인증 거부)
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }
}
