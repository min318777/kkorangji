package com.min.meow.user.dto.reponse;

import com.min.meow.global.Role;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String loginId;
    private Role role;
    private boolean rememberMe;
}
