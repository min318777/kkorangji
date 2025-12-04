package com.min.meow.user.dto.reponse;

import com.min.meow.global.Role;
import com.min.meow.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinResponse {

    private Long userId;

    private String loginId;

    private String email;

    private String name;

    private Role role;

    private LocalDateTime registeredAt;

    public static JoinResponse convertToDto(User user){

        return JoinResponse.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .email(user.getEmail())
                .registeredAt(user.getRegisteredAt())
                .role(user.getRole())
                .build();
    }

}
