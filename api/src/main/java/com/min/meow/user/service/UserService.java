package com.min.meow.user.service;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.user.dto.response.JoinResponse;
import com.min.meow.user.dto.response.LoginResponse;
import com.min.meow.user.dto.request.JoinRequest;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.user.entity.Role;
import com.min.meow.user.entity.User;
import com.min.meow.user.entity.UserRole;
import com.min.meow.security.service.PermissionCacheService;
import com.min.meow.security.service.RefreshTokenService;
import com.min.meow.user.repository.RoleRepository;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionCacheService permissionCacheService;

    public boolean isLoginIdAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    public LoginResponse login(LoginRequest loginRequest){
        User user = userRepository.findByLoginId(loginRequest.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.UNREGISTERED_USER));

        return LoginResponse.builder()
                .loginId(user.getLoginId())
                .rememberMe(loginRequest.isRememberMe())
                .build();
    }

    @Transactional
    public JoinResponse join(JoinRequest joinRequest){

        if (joinRequest.getEmail() != null && userRepository.existsByEmail(joinRequest.getEmail())) {
            throw new CustomException(ErrorCode.ALREADY_EXISTING_EMAIL);
        }
        if (userRepository.existsByLoginId(joinRequest.getLoginId())){
            throw new CustomException(ErrorCode.ALREADY_EXISTING_USER);
        }

        String encodedPassword = bCryptPasswordEncoder.encode(joinRequest.getPassword());

        User user = User.builder()
                .loginId(joinRequest.getLoginId())
                .password(encodedPassword)
                .nickname(joinRequest.getNickname())
                .email(joinRequest.getEmail())
                .isDelete(false)
                .build();
        userRepository.save(user);

        // 기본 역할(ROLE_USER) 부여
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ROLE));
        UserRole newUserRole = new UserRole(user, userRole);
        userRoleRepository.save(newUserRole);

        // userRoles 리스트에 추가하여 JoinResponse에서 역할 정보 반환 가능하도록 함
        user.getUserRoles().add(newUserRole);

        log.info("회원가입 완료 - userId: {}, loginId: {}", user.getId(), user.getLoginId());
        return JoinResponse.from(user);
    }

    /**
     * 회원 탈퇴 처리
     * 1. 사용자 존재 여부 확인
     * 2. 이미 탈퇴한 사용자인지 확인
     * 3. 리프레시 토큰 삭제 (로그인 세션 무효화)
     * 4. 개인정보 비식별화 및 소프트 삭제 처리
     * 소프트 삭제 방식을 사용하여:
     * - 게시글, 댓글 등 기존 데이터의 외래키 참조 무결성 유지
     * - 탈퇴 후에도 게시글은 "탈퇴한 사용자"로 표시되어 조회 가능
     * - 개인정보는 비식별화되어 개인정보보호법 준수
     * @param userId 탈퇴할 사용자의 ID (PK)
     */
    @Transactional
    public void withdraw(Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 2. 이미 탈퇴한 사용자인지 확인
        if (user.isWithdrawn()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN_USER);
        }

        // 3. 리프레시 토큰 삭제 - 모든 디바이스에서 로그아웃 처리
        refreshTokenService.delete(userId);

        // v3 권한 캐시 무효화 — 탈퇴 후 다음 요청에서 캐시 미스 → DB 확인 → 탈퇴 확인 → 차단
        permissionCacheService.evictPermissions(userId);

        log.info("회원 탈퇴 처리 - userId: {}, 리프레시 토큰 및 권한 캐시 삭제 완료", userId);

        // 4. 개인정보 비식별화 및 소프트 삭제 처리
        user.withdraw();
        userRepository.save(user);

        log.info("회원 탈퇴 완료 - userId: {}, 비식별화된 loginId: {}", user.getId(), user.getLoginId());
    }

    /**
     * 카카오 연결 해제(Unlink) 웹훅 처리
     * 사용자가 카카오 쪽(카카오톡 앱 설정)에서 서비스 연결을 끊으면 카카오가 이 정보를 알려주는데,
     * 해당 계정을 찾아 일반 탈퇴와 동일하게 비식별화 처리한다 (개인정보보호법 준수).
     * 이미 탈퇴했거나 존재하지 않는 계정이면 조용히 종료 (카카오 쪽 재시도 대응, 웹훅은 실패해도 안 됨)
     * @param socialId 카카오 고유 사용자 ID
     */
    @Transactional
    public void withdrawByKakaoUnlink(String socialId) {
        userRepository.findByProviderAndSocialId("kakao", socialId)
                .filter(user -> !user.isWithdrawn())
                .ifPresent(user -> {
                    refreshTokenService.delete(user.getId());
                    permissionCacheService.evictPermissions(user.getId());
                    user.withdraw();
                    userRepository.save(user);
                    log.info("카카오 연결 해제 웹훅으로 인한 탈퇴 처리 - userId: {}", user.getId());
                });
    }
}
