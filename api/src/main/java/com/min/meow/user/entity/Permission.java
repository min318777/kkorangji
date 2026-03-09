package com.min.meow.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 권한(Permission) 엔티티.
 * RBAC 모델에서 가장 세분화된 권한 단위를 표현한다.
 * 예: "post:read", "post:write", "user:manage"
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 권한 코드 (예: "post:read", "comment:write")
    @Column(unique = true, nullable = false, length = 50)
    private String code;

    // 권한 설명 (예: "게시글 조회")
    @Column(nullable = false, length = 100)
    private String description;

    public Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
