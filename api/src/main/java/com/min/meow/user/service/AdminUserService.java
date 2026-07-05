package com.min.meow.user.service;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.security.service.PermissionCacheService;
import com.min.meow.user.dto.reponse.AdminUserListResponse;
import com.min.meow.user.dto.reponse.AdminUserResponse;
import com.min.meow.user.entity.Role;
import com.min.meow.user.entity.User;
import com.min.meow.user.entity.UserRole;
import com.min.meow.security.service.RefreshTokenService;
import com.min.meow.user.repository.RoleRepository;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final PermissionCacheService permissionCacheService;

    /**
     * 사용자 제한: ROLE_USER → ROLE_RESTRICTED
     * 이후 요청부터 post:write, comment:write 권한 없음 → 작성/수정/댓글 불가
     */
    @Transactional
    public AdminUserResponse restrictUser(Long adminId, Long targetUserId) {
        if (adminId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_MANAGE_SELF);
        }

        User target = findActiveUser(targetUserId);

        // 관리자는 제한 불가
        if (target.getRoleNames().contains("ROLE_ADMIN")) {
            throw new CustomException(ErrorCode.CANNOT_MANAGE_ADMIN);
        }
        // 이미 제한 상태
        if (target.getRoleNames().contains("ROLE_RESTRICTED")) {
            throw new CustomException(ErrorCode.ALREADY_RESTRICTED_USER);
        }

        Role restricted = roleRepository.findByName("ROLE_RESTRICTED")
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ROLE));

        // 기존 역할 엔티티 조회 후 삭제 (JPQL 벌크 DELETE 대신 영속성 컨텍스트 경유)
        List<UserRole> existingRoles = userRoleRepository.findByUserId(targetUserId);
        userRoleRepository.deleteAll(existingRoles);
        userRoleRepository.flush();
        userRoleRepository.save(new UserRole(target, restricted));

        // Redis 권한 캐시 무효화 → 다음 요청부터 즉시 반영
        permissionCacheService.evictPermissions(targetUserId);

        log.info("사용자 제한 처리 완료 - adminId: {}, targetUserId: {}", adminId, targetUserId);

        // 변경 사항을 즉시 반영하여 응답 생성
        target.getUserRoles().clear();
        target.getUserRoles().add(new UserRole(target, restricted));
        return AdminUserResponse.from(target);
    }

    /**
     * 사용자 복원: ROLE_RESTRICTED → ROLE_USER
     * 이후 요청부터 post:write, comment:write 권한 복구
     */
    @Transactional
    public AdminUserResponse restoreUser(Long adminId, Long targetUserId) {
        if (adminId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_MANAGE_SELF);
        }

        User target = findActiveUser(targetUserId);

        // ROLE_RESTRICTED 상태가 아니면 복원 불가
        if (!target.getRoleNames().contains("ROLE_RESTRICTED")) {
            throw new CustomException(ErrorCode.NOT_RESTRICTED_USER);
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ROLE));

        // 기존 역할 엔티티 조회 후 삭제 (JPQL 벌크 DELETE 대신 영속성 컨텍스트 경유)
        List<UserRole> existingRoles = userRoleRepository.findByUserId(targetUserId);
        userRoleRepository.deleteAll(existingRoles);
        userRoleRepository.flush();
        userRoleRepository.save(new UserRole(target, userRole));

        // Redis 권한 캐시 무효화 → 다음 요청부터 즉시 반영
        permissionCacheService.evictPermissions(targetUserId);

        log.info("사용자 복원 처리 완료 - adminId: {}, targetUserId: {}", adminId, targetUserId);

        target.getUserRoles().clear();
        target.getUserRoles().add(new UserRole(target, userRole));
        return AdminUserResponse.from(target);
    }

    /**
     * 강제 탈퇴: 관리자가 특정 사용자를 소프트 삭제
     * 리프레시 토큰 삭제 + 권한 캐시 무효화 + 개인정보 비식별화
     */
    @Transactional
    public void forceWithdraw(Long adminId, Long targetUserId) {
        if (adminId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_MANAGE_SELF);
        }

        User target = findActiveUser(targetUserId);

        // 관리자는 강제 탈퇴 불가
        if (target.getRoleNames().contains("ROLE_ADMIN")) {
            throw new CustomException(ErrorCode.CANNOT_MANAGE_ADMIN);
        }

        // 리프레시 토큰 삭제 → 모든 디바이스 즉시 로그아웃
        refreshTokenService.delete(targetUserId);

        // Redis 권한 캐시 무효화
        permissionCacheService.evictPermissions(targetUserId);

        // 개인정보 비식별화 및 소프트 삭제
        target.withdraw();
        userRepository.save(target);

        log.info("강제 탈퇴 처리 완료 - adminId: {}, targetUserId: {}", adminId, targetUserId);
    }

    /**
     * 유저 목록 조회 (역할 필터 옵션, 페이징)
     * roleName이 null이면 전체 유저 반환
     */
    public Page<AdminUserListResponse> getUserList(String roleName, Pageable pageable) {
        Page<User> users = userRepository.findAllByOptionalRole(roleName, pageable);
        return users.map(AdminUserListResponse::from);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
        if (user.isWithdrawn()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN_USER);
        }
        return user;
    }
}
