package com.min.meow.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Schema(description = "로그인 요청")
@Getter
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

    // 식별자: 앞뒤 공백 제거
    public void setLoginId(String loginId) {
        this.loginId = loginId != null ? loginId.trim() : null;
    }

    // 비밀번호: 공백이 의도적일 수 있으므로 정규화 안 함
    public void setPassword(String password) {
        this.password = password;
    }

    // boolean: 정규화 불필요
    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
