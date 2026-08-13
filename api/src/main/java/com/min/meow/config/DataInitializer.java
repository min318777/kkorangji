package com.min.meow.config;

import com.min.meow.user.entity.Permission;
import com.min.meow.user.entity.Role;
import com.min.meow.user.entity.RolePermission;
import com.min.meow.user.repository.PermissionRepository;
import com.min.meow.user.repository.RolePermissionRepository;
import com.min.meow.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBAC 초기 데이터 생성기.
 * 로컬 환경에서 애플리케이션 시작 시 Role, Permission, RolePermission 데이터를 자동 생성한다.
 * 이미 데이터가 존재하면 스킵한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"local", "test"})  // 로컬 및 테스트 환경에서 실행
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (roleRepository.count() == 0) {
            initializeFromScratch();
        } else {
            // 기존 데이터가 있어도 ROLE_RESTRICTED 누락 시 추가
            addRestrictedRoleIfMissing();
        }
    }

    private void initializeFromScratch() {
        log.info("RBAC 초기 데이터 생성 시작");

        // ── Permission 생성 (10개) ──────────────────────────────────────
        // 게시글
        Permission postRead   = permissionRepository.save(new Permission("post:read",   "게시글 조회"));
        Permission postCreate = permissionRepository.save(new Permission("post:create", "게시글 작성"));
        Permission postUpdate = permissionRepository.save(new Permission("post:update", "게시글 수정"));
        Permission postDelete = permissionRepository.save(new Permission("post:delete", "게시글 삭제 (타인 포함)"));
        // 댓글
        Permission commentCreate = permissionRepository.save(new Permission("comment:create", "댓글 작성"));
        Permission commentUpdate = permissionRepository.save(new Permission("comment:update", "댓글 수정"));
        Permission commentDelete = permissionRepository.save(new Permission("comment:delete", "댓글 삭제 (타인 포함)"));
        // 유저 관리 (세분화)
        Permission userRead     = permissionRepository.save(new Permission("user:read",     "유저 목록/통계 조회"));
        Permission userRestrict = permissionRepository.save(new Permission("user:restrict", "유저 계정 제재/복원"));
        Permission userDelete   = permissionRepository.save(new Permission("user:delete",   "유저 강제 탈퇴"));

        // ── Role 생성 ────────────────────────────────────────────────────
        Role userRole     = roleRepository.save(new Role("ROLE_USER",     "일반 사용자"));
        Role adminRole    = roleRepository.save(new Role("ROLE_ADMIN",    "관리자"));
        Role viewerRole   = roleRepository.save(new Role("ROLE_VIEWER",   "뷰어 (콘텐츠 작성/수정 테스트용, 타인 글 삭제 불가)"));
        Role restrictedRole = roleRepository.save(new Role("ROLE_RESTRICTED", "제한된 사용자"));

        // ── ROLE_USER: 조회, 작성, 수정, 댓글 작성/수정 (삭제는 본인것만 서비스 레이어 isAuthor 체크) ──
        rolePermissionRepository.save(new RolePermission(userRole, postRead));
        rolePermissionRepository.save(new RolePermission(userRole, postCreate));
        rolePermissionRepository.save(new RolePermission(userRole, postUpdate));
        rolePermissionRepository.save(new RolePermission(userRole, commentCreate));
        rolePermissionRepository.save(new RolePermission(userRole, commentUpdate));

        // ── ROLE_VIEWER: 콘텐츠 작성/수정 가능, 삭제는 본인 것만(isAuthor 체크), 타인 글 삭제 불가 ──
        rolePermissionRepository.save(new RolePermission(viewerRole, postRead));
        rolePermissionRepository.save(new RolePermission(viewerRole, postCreate));
        rolePermissionRepository.save(new RolePermission(viewerRole, postUpdate));
        rolePermissionRepository.save(new RolePermission(viewerRole, commentCreate));
        rolePermissionRepository.save(new RolePermission(viewerRole, commentUpdate));
        rolePermissionRepository.save(new RolePermission(viewerRole, userRead));

        // ── ROLE_ADMIN: 모든 권한 10개 ──
        rolePermissionRepository.save(new RolePermission(adminRole, postRead));
        rolePermissionRepository.save(new RolePermission(adminRole, postCreate));
        rolePermissionRepository.save(new RolePermission(adminRole, postUpdate));
        rolePermissionRepository.save(new RolePermission(adminRole, postDelete));
        rolePermissionRepository.save(new RolePermission(adminRole, commentCreate));
        rolePermissionRepository.save(new RolePermission(adminRole, commentUpdate));
        rolePermissionRepository.save(new RolePermission(adminRole, commentDelete));
        rolePermissionRepository.save(new RolePermission(adminRole, userRead));
        rolePermissionRepository.save(new RolePermission(adminRole, userRestrict));
        rolePermissionRepository.save(new RolePermission(adminRole, userDelete));

        // ── ROLE_RESTRICTED: 조회만 가능 (제재된 사용자) ──
        rolePermissionRepository.save(new RolePermission(restrictedRole, postRead));

        log.info("RBAC 초기 데이터 생성 완료 - Permission: 10개, Role: 4개");
    }

    /** 기존 DB에 ROLE_RESTRICTED가 없을 때만 추가 (멱등성 보장) */
    private void addRestrictedRoleIfMissing() {
        if (roleRepository.findByName("ROLE_RESTRICTED").isPresent()) {
            log.info("RBAC 초기 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        Permission postRead = permissionRepository.findByCode("post:read")
                .orElseGet(() -> permissionRepository.save(new Permission("post:read", "게시글 조회")));

        Role restrictedRole = roleRepository.save(new Role("ROLE_RESTRICTED", "제한된 사용자"));
        rolePermissionRepository.save(new RolePermission(restrictedRole, postRead));

        log.info("ROLE_RESTRICTED 역할 추가 완료");
    }
}
