package com.min.meow.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 역할(Role) 엔티티.
 * 기존 enum Role을 대체하며, RolePermission을 통해 Permission과 N:M 관계를 가진다.
 * name 예시: "ROLE_USER", "ROLE_ADMIN"
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 역할 이름 (예: "ROLE_USER", "ROLE_ADMIN")
    @Column(unique = true, nullable = false, length = 30)
    private String name;

    // 역할 설명 (예: "일반 사용자")
    @Column(nullable = false, length = 100)
    private String description;

    // Role → RolePermission 관계 (이 역할이 가진 권한 목록)
    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER)
    private List<RolePermission> rolePermissions = new ArrayList<>();

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 이 역할이 특정 권한 코드를 가지고 있는지 확인
     */
    public boolean hasPermission(String code) {
        return rolePermissions.stream()
                .anyMatch(rp -> rp.getPermission().getCode().equals(code));
    }

    /**
     * 이 역할이 가진 모든 권한 코드를 반환
     */
    public Set<String> getPermissionCodes() {
        return rolePermissions.stream()
                .map(rp -> rp.getPermission().getCode())
                .collect(Collectors.toSet());
    }
}
