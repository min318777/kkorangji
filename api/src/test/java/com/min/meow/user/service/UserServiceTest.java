package com.min.meow.user.service;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.security.service.PermissionCacheService;
import com.min.meow.security.service.RefreshTokenService;
import com.min.meow.user.dto.response.JoinResponse;
import com.min.meow.user.dto.request.JoinRequest;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.user.entity.Role;
import com.min.meow.user.entity.User;
import com.min.meow.user.entity.UserRole;
import com.min.meow.user.repository.RoleRepository;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * UserService 유닛 테스트 — 회원가입, 로그인, 회원 탈퇴 비즈니스 로직
 *
 * <h3>FastAPI 매핑</h3>
 * <pre>
 * FastAPI (test_auth.py)                Java (UserServiceTest)
 * ─────────────────────────────────     ──────────────────────────────────────
 * pytest + unittest.mock.patch      →   @ExtendWith(MockitoExtension) + @Mock
 * dependency_overrides[get_session] →   Mock 객체 반환값 설정 (given(...).willReturn())
 * assert response.status_code       →   assertThatThrownBy(...).extracting("errorCode")
 * </pre>
 *
 * <h3>통합 테스트(UserControllerTest)와의 차이</h3>
 * <pre>
 * 통합 테스트                        유닛 테스트
 * 실제 HTTP 요청 + 전체 필터 체인  →  순수 Java 메서드 호출
 * H2 DB                          →  Mock 객체 (DB 없음)
 * 느림 (Spring 컨텍스트 로딩)     →  빠름 (Mockito만 사용)
 * 외부 의존성(@Valid, Filter) 검증 →  비즈니스 로직만 검증
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 유닛 테스트")
class UserServiceTest {

    /** 테스트 대상 — 모든 의존성은 아래 @Mock으로 자동 주입 */
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PermissionCacheService permissionCacheService;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    // =========================================================================
    // 테스트 픽스처 헬퍼 — 반복 생성 공통화
    // =========================================================================

    /** 기본 테스트용 JoinRequest 생성 */
    private JoinRequest createJoinRequest(String loginId, String email, String nickname) {
        JoinRequest req = new JoinRequest();
        req.setLoginId(loginId);
        req.setPassword("password1!");
        req.setPasswordConfirm("password1!");
        req.setEmail(email);
        req.setNickname(nickname);
        return req;
    }

    /** 기본 테스트용 User 생성 (DB 없이 순수 Java 객체) */
    private User createTestUser(String loginId, String email, String nickname) {
        return User.builder()
                .loginId(loginId)
                .email(email)
                .nickname(nickname)
                .password("$2a$10$encoded")   // BCrypt 형태 더미 값
                .isDelete(false)
                .userRoles(new ArrayList<>())
                .build();
    }

    /** 기본 테스트용 Role 생성 */
    private Role createTestRole() {
        // Role은 @Builder 없이 생성자만 사용 (name, description 파라미터)
        return new Role("ROLE_USER", "일반 사용자");
    }

    // =========================================================================
    // join() — 회원가입
    // FastAPI: class TestRegister
    // =========================================================================
    @Nested
    @DisplayName("join() — 회원가입")
    class Join {

        @Test
        @DisplayName("성공: 유효한 정보로 회원가입하면 JoinResponse를 반환하고 User를 저장한다")
        void test_성공_회원가입() {
            // FastAPI: test_성공_회원가입
            // given
            JoinRequest request = createJoinRequest("newcat01", "newcat@example.com", "냥이");

            // 이메일/아이디 중복 없음
            given(userRepository.existsByEmail("newcat@example.com")).willReturn(false);
            given(userRepository.existsByLoginId("newcat01")).willReturn(false);

            // 비밀번호 인코딩 Mock
            given(bCryptPasswordEncoder.encode("password1!")).willReturn("$2a$10$encoded");

            // User 저장 Mock
            User savedUser = createTestUser("newcat01", "newcat@example.com", "냥이");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // Role 조회 Mock
            Role userRole = createTestRole();
            given(roleRepository.findByName("ROLE_USER")).willReturn(Optional.of(userRole));

            // UserRole 저장 Mock
            given(userRoleRepository.save(any(UserRole.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            JoinResponse response = userService.join(request);

            // then — 응답에 비밀번호가 없어야 함 (보안 핵심)
            assertThat(response.getLoginId()).isEqualTo("newcat01");
            assertThat(response.getNickname()).isEqualTo("냥이");
            assertThat(response.getEmail()).isEqualTo("newcat@example.com");

            // userRepository.save()가 정확히 1회 호출되어야 함
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 이메일로 가입하면 CustomException(ALREADY_EXISTING_EMAIL)을 던진다")
        void test_실패_이메일_중복() {
            // FastAPI: test_실패_이메일_중복
            // given — 이메일 중복
            JoinRequest request = createJoinRequest("newcat01", "dup@example.com", "냥이");
            given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

            // when & then — ALREADY_EXISTING_EMAIL 예외 발생
            assertThatThrownBy(() -> userService.join(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_EXISTING_EMAIL);

            // 중복 이메일 시 저장이 호출되지 않아야 함
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이미 존재하는 loginId로 가입하면 CustomException(ALREADY_EXISTING_USER)을 던진다")
        void test_실패_아이디_중복() {
            // given — 이메일은 중복 없음, loginId는 중복
            JoinRequest request = createJoinRequest("dupcat01", "unique@example.com", "냥이");
            given(userRepository.existsByEmail("unique@example.com")).willReturn(false);
            given(userRepository.existsByLoginId("dupcat01")).willReturn(true);

            // when & then — ALREADY_EXISTING_USER 예외 발생
            assertThatThrownBy(() -> userService.join(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_EXISTING_USER);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: DB에 ROLE_USER가 없으면 CustomException(NOT_FOUND_ROLE)을 던진다")
        void test_실패_역할_없음() {
            // given — 중복 없음 + Role이 DB에 없음 (서버 설정 오류 시나리오)
            JoinRequest request = createJoinRequest("norole01", "norole@example.com", "냥이");
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(userRepository.existsByLoginId(anyString())).willReturn(false);
            given(bCryptPasswordEncoder.encode(anyString())).willReturn("$2a$10$encoded");
            given(userRepository.save(any(User.class))).willReturn(createTestUser("norole01", "norole@example.com", "냥이"));
            given(roleRepository.findByName("ROLE_USER")).willReturn(Optional.empty()); // Role 없음

            // when & then — NOT_FOUND_ROLE 예외 발생
            assertThatThrownBy(() -> userService.join(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_ROLE);
        }
    }

    // =========================================================================
    // login() — 로그인 (서비스 레이어 — 실제 인증은 Spring Security가 담당)
    // FastAPI: class TestLogin
    // =========================================================================
    @Nested
    @DisplayName("login() — 로그인 서비스 레이어")
    class Login {

        @Test
        @DisplayName("성공: 존재하는 loginId로 호출하면 LoginResponse를 반환한다")
        void test_성공_로그인_서비스() {
            // FastAPI: test_성공_로그인
            // 참고: 실제 비밀번호 검증은 Spring Security AuthenticationManager가 담당.
            // UserService.login()은 User 조회 + LoginResponse 생성만 수행한다.

            // given
            User user = createTestUser("logincat1", "login@example.com", "로그인냥이");
            given(userRepository.findByLoginId("logincat1")).willReturn(Optional.of(user));

            LoginRequest request = new LoginRequest();
            request.setLoginId("logincat1");
            request.setPassword("password1!");

            // when
            var response = userService.login(request);

            // then
            assertThat(response.getLoginId()).isEqualTo("logincat1");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 loginId로 호출하면 CustomException(UNREGISTERED_USER)을 던진다")
        void test_실패_존재하지_않는_아이디() {
            // FastAPI: test_실패_존재하지_않는_이메일
            // 핵심: 아이디 없음 = UNREGISTERED_USER(401) 반환 → 계정 존재 여부 비공개

            // given — DB에 없는 loginId
            given(userRepository.findByLoginId("nonexist1")).willReturn(Optional.empty());

            LoginRequest request = new LoginRequest();
            request.setLoginId("nonexist1");
            request.setPassword("anypassword1!");

            // when & then — UNREGISTERED_USER 예외 발생
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.UNREGISTERED_USER);
        }
    }

    // =========================================================================
    // withdraw() — 회원 탈퇴 (우리 프로젝트 고유 기능)
    // =========================================================================
    @Nested
    @DisplayName("withdraw() — 회원 탈퇴")
    class Withdraw {

        @Test
        @DisplayName("성공: 유효한 사용자가 탈퇴하면 소프트 삭제와 토큰 삭제가 처리된다")
        void test_성공_회원_탈퇴() {
            // given — 정상 사용자 (탈퇴 안 된 상태)
            User user = spy(createTestUser("withdraw1", "withdraw@example.com", "탈퇴냥이"));
            given(user.isWithdrawn()).willReturn(false);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.save(any(User.class))).willReturn(user);

            // when
            userService.withdraw(1L);

            // then — 리프레시 토큰 삭제 + 소프트 삭제 처리 확인
            verify(refreshTokenService, times(1)).delete(1L);
            verify(user, times(1)).withdraw();
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 userId로 탈퇴 요청하면 CustomException(NOT_FOUND_USER)을 던진다")
        void test_실패_존재하지_않는_사용자_탈퇴() {
            // given — DB에 없는 User
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then — NOT_FOUND_USER 예외 발생
            assertThatThrownBy(() -> userService.withdraw(999L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_USER);

            // 토큰 삭제와 저장이 호출되지 않아야 함
            verify(refreshTokenService, never()).delete(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이미 탈퇴한 사용자가 다시 탈퇴 요청하면 CustomException(ALREADY_WITHDRAWN_USER)을 던진다")
        void test_실패_이미_탈퇴한_사용자() {
            // given — 이미 탈퇴한 User
            User user = spy(createTestUser("already1", "already@example.com", "탈퇴된냥이"));
            given(user.isWithdrawn()).willReturn(true);  // 이미 탈퇴 상태
            given(userRepository.findById(2L)).willReturn(Optional.of(user));

            // when & then — ALREADY_WITHDRAWN_USER 예외 발생
            assertThatThrownBy(() -> userService.withdraw(2L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_WITHDRAWN_USER);

            // 실제 탈퇴 처리(토큰 삭제, 저장)가 호출되지 않아야 함
            verify(refreshTokenService, never()).delete(any());
            verify(userRepository, never()).save(any());
        }
    }
}