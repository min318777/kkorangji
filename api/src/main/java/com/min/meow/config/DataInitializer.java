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
@Profile("local")  // 로컬 환경에서만 실행
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (roleRepository.count() > 0) {
            log.info("RBAC 초기 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        log.info("RBAC 초기 데이터 생성 시작");

        // Permission 생성
        Permission postRead = permissionRepository.save(new Permission("post:read", "게시글 조회"));
        Permission postWrite = permissionRepository.save(new Permission("post:write", "게시글 작성"));
        Permission postDelete = permissionRepository.save(new Permission("post:delete", "게시글 삭제"));
        Permission commentWrite = permissionRepository.save(new Permission("comment:write", "댓글 작성"));
        Permission commentDelete = permissionRepository.save(new Permission("comment:delete", "댓글 삭제"));
        Permission userManage = permissionRepository.save(new Permission("user:manage", "사용자 관리"));

        // Role 생성
        Role userRole = roleRepository.save(new Role("ROLE_USER", "일반 사용자"));
        Role adminRole = roleRepository.save(new Role("ROLE_ADMIN", "관리자"));

        // ROLE_USER: 조회, 작성, 댓글
        rolePermissionRepository.save(new RolePermission(userRole, postRead));
        rolePermissionRepository.save(new RolePermission(userRole, postWrite));
        rolePermissionRepository.save(new RolePermission(userRole, commentWrite));

        // ROLE_ADMIN: 모든 권한
        rolePermissionRepository.save(new RolePermission(adminRole, postRead));
        rolePermissionRepository.save(new RolePermission(adminRole, postWrite));
        rolePermissionRepository.save(new RolePermission(adminRole, postDelete));
        rolePermissionRepository.save(new RolePermission(adminRole, commentWrite));
        rolePermissionRepository.save(new RolePermission(adminRole, commentDelete));
        rolePermissionRepository.save(new RolePermission(adminRole, userManage));

        log.info("RBAC 초기 데이터 생성 완료 - Permission: 6개, Role: 2개");
    }
}
