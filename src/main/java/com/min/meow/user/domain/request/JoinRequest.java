package com.min.meow.user.domain.request;

import com.min.meow.global.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRequest {

    @NotBlank(message = "아이디를 입력해 주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호를 한번 더 입력해 주세요.")
    private String passwordConfirm;

    @NotBlank(message = "이메일을 입력해 주세요.")
    private String email;

    @NotBlank(message = "이름을 입력해 주세요.")
    private String name;

    @NotNull(message = "권한을 선택해 주세요.")
    private Role role;
}
