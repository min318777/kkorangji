package com.min.meow.user.dto.reponse;

import com.min.meow.global.Role;
import com.min.meow.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Schema(description = "회원가입 응답")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "로그인 ID", example = "cat_lover")
    private String loginId;

    @Schema(description = "이메일", example = "cat_lover@example.com")
    private String email;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "사용자 역할", example = "USER")
    private Role role;

    @Schema(description = "가입일시", example = "2025-01-15T10:30:00")
    private LocalDateTime registeredAt;

    public static JoinResponse from(User user){

        return JoinResponse.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .email(user.getEmail())
                .registeredAt(user.getRegisteredAt())
                .role(user.getRole())
                .build();
    }

}
