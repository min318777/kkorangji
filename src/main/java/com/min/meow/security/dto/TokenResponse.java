package com.min.meow.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 토큰 재발급 응답 DTO
 */
@Schema(description = "토큰 재발급 응답")
@Getter
@AllArgsConstructor
public class TokenResponse {

    @Schema(description = "새 Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "새 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
