package com.min.meow.user.domain.request;

import com.min.meow.global.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRequest {

    @NotBlank(message = "아이디를 입력해 주세요.")
   // @Size(min = 8, max = 20, message = "최소 8자 이상, 최대 20자 이하로 입력해 주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    //@Size(min = 8, max = 20, message = "최소 8자 이상, 최대 20자 이하로 입력해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호를 한번 더 입력해 주세요.")
    //@Size(min = 8, max = 20, message = "최소 8자 이상, 최대 20자 이하로 입력해 주세요.")
    private String passwordConfirm;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바르지 않는 이메일 형식입니다.")
    private String email;

    @NotBlank(message = "이름을 입력해 주세요.")
    private String name;

    //@NotNull(message = "권한을 선택해 주세요.")
    private Role role;
}
