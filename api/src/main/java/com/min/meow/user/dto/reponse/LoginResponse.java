package com.min.meow.user.dto.reponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "로그인 응답")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    @Schema(description = "로그인 ID", example = "cat_lover")
    private String loginId;

    @Schema(description = "사용자 역할", example = "ROLE_USER")
    private String role;

    @Schema(description = "자동 로그인 여부", example = "false")
    private boolean rememberMe;
}
