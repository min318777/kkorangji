package com.min.meow.user.repository;

import com.min.meow.user.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    // (role_id, permission_id) 조합으로 중복 여부 확인 — findAll() 없이 DB 레벨에서 처리
    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
