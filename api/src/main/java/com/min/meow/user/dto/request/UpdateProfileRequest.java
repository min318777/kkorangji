package com.min.meow.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 프로필 수정 요청 DTO
 * DTO 수준에서 수정 가능 필드를 원천 차단 — nickname만 정의
 * email, password 등 민감 필드는 별도 API로 처리
 */
@Schema(description = "프로필 수정 요청")
@Getter
public class UpdateProfileRequest {

    @Schema(description = "닉네임 (1~10자, 한글/영문)", example = "김냥이")
    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(min = 1, max = 10, message = "닉네임은 1자 이상 10자 이하로 입력해 주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "닉네임은 한글과 영문만 사용 가능합니다.")
    private String nickname;

    // 닉네임: 앞뒤 공백 제거
    public void setNickname(String nickname) {
        this.nickname = nickname != null ? nickname.trim() : null;
    }
}
