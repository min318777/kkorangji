package com.min.meow.user.entity;


import com.min.meow.global.Role;
import com.min.meow.post.postlike.entity.PostLike;
import com.min.meow.user.domain.request.JoinRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private Long id;

    @Column(unique = true, nullable = false)
    private String loginId;

    private String password;

    @Column(nullable = false, unique = true)
    @Email
    private String email;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostLike> postLike = new ArrayList<>();

    //@Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime registeredAt;
    private LocalDateTime lastLoginAt;
    private boolean isDelete;

    @PrePersist
    public void prePersist(){
        this.registeredAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){
        this.lastLoginAt = LocalDateTime.now();
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
