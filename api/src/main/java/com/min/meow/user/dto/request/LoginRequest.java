package com.min.meow.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "로그인 요청")
@Getter
@Setter
public class LoginRequest {

    // 로그인 시에는 @Pattern 불필요 (DB에 저장된 값과 비교만 하면 됨)
    @Schema(description = "로그인 ID", example = "catlover01")
    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(min = 5, max = 20, message = "아이디는 5자 이상 20자 이하로 입력해 주세요.")
    private String loginId;

    @Schema(description = "비밀번호", example = "password1!")
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    //@Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해 주세요.")
    private String password;

    @Schema(description = "로그인 상태 유지 여부", example = "false")
    private boolean rememberMe;
}
