package com.min.meow.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;

@Schema(description = "회원가입 요청")
@Getter
public class JoinRequest {

    // 아이디: 5~20자, 영문/숫자만 허용
    @Schema(description = "로그인 ID (5~20자, 영문/숫자)", example = "catlover01")
    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(min = 5, max = 20, message = "아이디는 5자 이상 20자 이하로 입력해 주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문과 숫자만 사용 가능합니다.")
    private String loginId;

    // 비밀번호: 8~100자, 영문+숫자 필수 포함, 특수문자 허용
    @Schema(description = "비밀번호 (영문+숫자 필수 포함)", example = "password1!")
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    //@Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해 주세요.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]+$",
            message = "비밀번호는 영문과 숫자를 반드시 포함해야 합니다."
    )
    private String password;

    // 비밀번호 확인
    @Schema(description = "비밀번호 확인 (password와 동일해야 함)", example = "password1!")
    @NotBlank(message = "비밀번호를 한번 더 입력해 주세요.")
    private String passwordConfirm;

    // 이메일: 올바른 형식
    @Schema(description = "이메일 주소", example = "catlover@example.com")
    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바르지 않은 이메일 형식입니다.")
    private String email;

    // 닉네임: 1~10자, 한글/영문만 허용
    @Schema(description = "닉네임 (1~10자, 한글/영문)", example = "김냥이")
    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(min = 1, max = 10, message = "닉네임은 1자 이상 10자 이하로 입력해 주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "닉네임은 한글과 영문만 사용 가능합니다.")
    private String nickname;

    // 필드 간 관계 검증: 비밀번호와 비밀번호 확인이 일치하는지 확인
    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    @Schema(hidden = true)
    private boolean isPasswordMatching() {
        if (password == null || passwordConfirm == null) return true;
        return password.equals(passwordConfirm);
    }

    // 식별자: 앞뒤 공백 제거
    public void setLoginId(String loginId) {
        this.loginId = loginId != null ? loginId.trim() : null;
    }

    // 이메일: 앞뒤 공백 제거 + 소문자 변환 (이메일은 대소문자 구분 없음이 표준)
    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    // 닉네임: 앞뒤 공백 제거
    public void setNickname(String nickname) {
        this.nickname = nickname != null ? nickname.trim() : null;
    }

    // 비밀번호: 공백이 의도적일 수 있으므로 정규화 안 함
    public void setPassword(String password) {
        this.password = password;
    }

    // 비밀번호 확인: 정규화 안 함
    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
