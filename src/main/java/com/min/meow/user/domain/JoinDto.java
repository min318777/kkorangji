package com.min.meow.user.domain;

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

    private String nickName;

    private Role role;

    private LocalDateTime registeredAt;

    public static JoinDto convertToDto(User user){

        return JoinDto.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .email(user.getEmail())
                .nickName(user.getNickName())
                .registeredAt(user.getRegisteredAt())
                .role(user.getRole())
                .build();
    }

}
