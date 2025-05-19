package com.min.meow.user.entity;


import com.min.meow.global.Role;
import com.min.meow.user.domain.request.JoinRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String loginId;

    private String password;

    @Column(nullable = false, unique = true)
    @Email
    private String email;

    @Column(nullable = false)
    private String name;

    //@Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime registeredAt;
    private LocalDateTime lastLoginAt;
    private boolean isDelete;

    @PrePersist
    public void prePersist(){
        this.registeredAt = LocalDateTime.now();
    }

    public static User convertToEntity(JoinRequest joinRequest){

        return User.builder()
                .loginId(joinRequest.getLoginId())
                .name(joinRequest.getName())
                .email(joinRequest.getEmail())
                .role(joinRequest.getRole())
                .isDelete(false)
                .build();
    }
}
