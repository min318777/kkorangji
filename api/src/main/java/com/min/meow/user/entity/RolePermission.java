package com.min.meow.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Role ↔ Permission 중간 엔티티.
 * 한 역할이 여러 권한을 가질 수 있고, 한 권한이 여러 역할에 부여될 수 있다.
 * (role_id, permission_id) 복합 유니크 제약으로 중복 부여 방지.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "role_permission",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"})
)
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 역할
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // 권한
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    public RolePermission(Role role, Permission permission) {
        this.role = role;
        this.permission = permission;
    }
}
