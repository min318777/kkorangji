package com.min.meow.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 토큰 재발급 응답 DTO
 */
@Getter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
}
