package com.min.meow.user.domain.dto;

import com.min.meow.global.Role;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginDto {

    private String loginId;
    private Role role;
}
