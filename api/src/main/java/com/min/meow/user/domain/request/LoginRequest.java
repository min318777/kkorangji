package com.min.meow.user.domain.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "아이디를 입력해 주세요.")
    //@Size(min = 8, max = 20, message = "최소 8자 이상, 최대 20자 이하를 입력해 주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    //@Size(min = 8, max = 20, message = "최소 8자 이상, 최대 20자 이하를 입력해 주세요.")
    private String password;

    private boolean rememberMe;
}
