package com.min.meow.user.domain.dto;

import com.min.meow.global.Role;
import com.min.meow.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinDto {

    private Long userId;

    private String loginId;

    private String email;

    private String name;

    private Role role;

    private LocalDateTime registeredAt;

    public static JoinDto convertToDto(User user){

        return JoinDto.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .email(user.getEmail())
                .registeredAt(user.getRegisteredAt())
                .role(user.getRole())
                .build();
    }

}
