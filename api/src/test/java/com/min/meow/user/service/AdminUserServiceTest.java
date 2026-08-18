package com.min.meow.user.service;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.security.service.PermissionCacheService;
import com.min.meow.security.service.RefreshTokenService;
import com.min.meow.user.dto.response.AdminUserResponse;
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
@DisplayName("AdminUserService 유닛 테스트")
class AdminUserServiceTest {

    @InjectMocks
    private AdminUserService adminUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PermissionCacheService permissionCacheService;

    private User createUserWithRole(Long id, String roleName) {
        User user = User.builder()
                .id(id)
                .loginId("user" + id)
                .nickname("냥이" + id)
                .password("$2a$10$encoded")
                .isDelete(false)
                .userRoles(new ArrayList<>())
                .build();
        user.getUserRoles().add(new UserRole(user, new Role(roleName, "설명")));
        return user;
    }

    @Nested
    @DisplayName("restrictUser() — 사용자 제한")
    class RestrictUser {

        @Test
        @DisplayName("성공: ROLE_USER를 ROLE_RESTRICTED로 변경하고 권한 캐시를 무효화한다")
        void test_성공_사용자_제한() {
            // given
            Long adminId = 1L;
            Long targetId = 2L;
            User target = createUserWithRole(targetId, "ROLE_USER");
            Role restrictedRole = new Role("ROLE_RESTRICTED", "제한된 사용자");

            given(userRepository.findById(targetId)).willReturn(Optional.of(target));
            given(roleRepository.findByName("ROLE_RESTRICTED")).willReturn(Optional.of(restrictedRole));
            given(userRoleRepository.findByUserId(targetId)).willReturn(List.of());

            // when
            AdminUserResponse response = adminUserService.restrictUser(adminId, targetId);

            // then
            assertThat(response).isNotNull();
            then(permissionCacheService).should().evictPermissions(targetId);
            then(userRoleRepository).should().save(any(UserRole.class));
        }

        @Test
        @DisplayName("실패: 본인 계정은 제한할 수 없다")
        void test_실패_본인_제한_불가() {
            // when & then
            assertThatThrownBy(() -> adminUserService.restrictUser(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CANNOT_MANAGE_SELF);

            then(userRepository).should(never()).findById(any());
        }

        @Test
        @DisplayName("실패: 관리자 계정은 제한할 수 없다")
        void test_실패_관리자_제한_불가() {
            // given
            Long targetId = 2L;
            User admin = createUserWithRole(targetId, "ROLE_ADMIN");
            given(userRepository.findById(targetId)).willReturn(Optional.of(admin));

            // when & then
            assertThatThrownBy(() -> adminUserService.restrictUser(1L, targetId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CANNOT_MANAGE_ADMIN);
        }

        @Test
        @DisplayName("실패: 이미 제한된 사용자면 ALREADY_RESTRICTED_USER 예외를 던진다")
        void test_실패_이미_제한됨() {
            // given
            Long targetId = 2L;
            User restricted = createUserWithRole(targetId, "ROLE_RESTRICTED");
            given(userRepository.findById(targetId)).willReturn(Optional.of(restricted));

            // when & then
            assertThatThrownBy(() -> adminUserService.restrictUser(1L, targetId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_RESTRICTED_USER);
        }

        @Test
        @DisplayName("실패: 이미 탈퇴한 사용자는 관리할 수 없다")
        void test_실패_이미_탈퇴한_사용자() {
            // given
            Long targetId = 2L;
            User withdrawn = User.builder()
                    .id(targetId).loginId("deleted").nickname("탈퇴함").password("pw")
                    .isDelete(true).userRoles(new ArrayList<>()).build();
            given(userRepository.findById(targetId)).willReturn(Optional.of(withdrawn));

            // when & then
            assertThatThrownBy(() -> adminUserService.restrictUser(1L, targetId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_WITHDRAWN_USER);
        }
    }

    @Nested
    @DisplayName("restoreUser() — 사용자 복원")
    class RestoreUser {

        @Test
        @DisplayName("성공: ROLE_RESTRICTED를 ROLE_USER로 복원하고 권한 캐시를 무효화한다")
        void test_성공_사용자_복원() {
            // given
            Long targetId = 2L;
            User restricted = createUserWithRole(targetId, "ROLE_RESTRICTED");
            Role userRole = new Role("ROLE_USER", "일반 사용자");

            given(userRepository.findById(targetId)).willReturn(Optional.of(restricted));
            given(roleRepository.findByName("ROLE_USER")).willReturn(Optional.of(userRole));
            given(userRoleRepository.findByUserId(targetId)).willReturn(List.of());

            // when
            adminUserService.restoreUser(1L, targetId);

            // then
            then(permissionCacheService).should().evictPermissions(targetId);
        }

        @Test
        @DisplayName("실패: 제한 상태가 아닌 사용자는 복원할 수 없다")
        void test_실패_제한_상태_아님() {
            // given
            Long targetId = 2L;
            User normal = createUserWithRole(targetId, "ROLE_USER");
            given(userRepository.findById(targetId)).willReturn(Optional.of(normal));

            // when & then
            assertThatThrownBy(() -> adminUserService.restoreUser(1L, targetId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_RESTRICTED_USER);
        }

        @Test
        @DisplayName("실패: 본인 계정은 복원 대상으로 지정할 수 없다")
        void test_실패_본인_복원_불가() {
            // when & then
            assertThatThrownBy(() -> adminUserService.restoreUser(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CANNOT_MANAGE_SELF);
        }
    }

    @Nested
    @DisplayName("forceWithdraw() — 강제 탈퇴")
    class ForceWithdraw {

        @Test
        @DisplayName("성공: 리프레시 토큰 삭제와 권한 캐시 무효화 후 비식별화 처리한다")
        void test_성공_강제_탈퇴() {
            // given
            Long targetId = 2L;
            User target = createUserWithRole(targetId, "ROLE_USER");
            given(userRepository.findById(targetId)).willReturn(Optional.of(target));

            // when
            adminUserService.forceWithdraw(1L, targetId);

            // then
            then(refreshTokenService).should().delete(targetId);
            then(permissionCacheService).should().evictPermissions(targetId);
            assertThat(target.isWithdrawn()).isTrue();
        }

        @Test
        @DisplayName("실패: 관리자 계정은 강제 탈퇴시킬 수 없다")
        void test_실패_관리자_강제탈퇴_불가() {
            // given
            Long targetId = 2L;
            User admin = createUserWithRole(targetId, "ROLE_ADMIN");
            given(userRepository.findById(targetId)).willReturn(Optional.of(admin));

            // when & then
            assertThatThrownBy(() -> adminUserService.forceWithdraw(1L, targetId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CANNOT_MANAGE_ADMIN);

            then(refreshTokenService).should(never()).delete(any());
        }

        @Test
        @DisplayName("실패: 본인 계정은 강제 탈퇴시킬 수 없다")
        void test_실패_본인_강제탈퇴_불가() {
            // when & then
            assertThatThrownBy(() -> adminUserService.forceWithdraw(1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CANNOT_MANAGE_SELF);
        }
    }
}
