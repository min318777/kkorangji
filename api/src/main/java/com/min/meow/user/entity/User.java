package com.min.meow.user.entity;

import com.min.meow.global.Role;
import com.min.meow.postlike.entity.PostLike;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String loginId;

    @Column(length = 100)
    private String password;

    @Column(nullable = false, unique = true)
    @Email
    private String email;

    @Column(nullable = false, length = 10)
    private String name;

    // CASCADE 제거: User는 소프트 삭제 정책이므로 실제 삭제가 발생하지 않음
    // DB 레벨 FK RESTRICT가 마지막 방어선 역할 수행
    @OneToMany(mappedBy = "user")
    private List<PostLike> postLike = new ArrayList<>();

    // DB 레벨 제약: 역할은 반드시 존재해야 함 (ROLE_USER, ROLE_ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private LocalDateTime registeredAt;
    private LocalDateTime lastLoginAt;

    // 탈퇴 관련 필드
    private boolean isDelete;           // 소프트 삭제 여부
    private LocalDateTime deletedAt;    // 탈퇴 시점
    private LocalDateTime anonymizedAt; // 개인정보 비식별화 시점

    @PrePersist
    public void prePersist(){
        this.registeredAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){
        this.lastLoginAt = LocalDateTime.now();
    }

    // 회원 탈퇴 (논리적 삭제)
    public void delete() {
        this.isDelete = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 회원 탈퇴 및 개인정보 비식별화 처리
     *
     * 소프트 삭제 방식으로 데이터 무결성을 유지하면서
     * 개인정보를 비식별화하여 개인정보보호법을 준수합니다.
     *
     * 비식별화 항목:
     * - loginId: "deleted_" + UUID (유니크 제약조건 유지)
     * - email: UUID + "@deleted.meow.com" (유니크 제약조건 유지)
     * - name: "탈퇴한 사용자"
     * - password: null
     */
    public void withdraw() {
        // 소프트 삭제 처리
        this.isDelete = true;
        this.deletedAt = LocalDateTime.now();
        this.anonymizedAt = LocalDateTime.now();

        // 개인정보 비식별화 - UUID로 고유성 유지
        String anonymousId = UUID.randomUUID().toString().substring(0, 8);
        this.loginId = "deleted_" + anonymousId;
        this.email = anonymousId + "@deleted.meow.com";
        this.name = "탈퇴한 사용자";
        this.password = null;
    }

    public boolean isWithdrawn() {
        return this.isDelete;
    }

    // 마지막 로그인 시간 업데이트
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
