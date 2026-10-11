package com.min.meow.security.oauth2;

import com.min.meow.security.RefreshCookieProvider;
import com.min.meow.security.jwt.JwtProvider;
import com.min.meow.security.service.PermissionCacheService;
import com.min.meow.security.service.RefreshTokenService;
import com.min.meow.user.entity.User;
import com.min.meow.user.service.DauService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final PermissionCacheService permissionCacheService;
    private final DauService dauService;
    private final RefreshCookieProvider refreshCookieProvider;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        User user = customUserDetails.getUser();
        Long userId = user.getId();
        String role = user.getPrimaryRoleName();
        List<String> permissions = List.copyOf(user.getAllPermissionCodes());

        String accessToken = jwtProvider.createAccessToken(userId, role, permissions);
        JwtProvider.RefreshTokenInfo refreshInfo = jwtProvider.createRefreshToken(userId);

        refreshTokenService.save(userId, refreshInfo.jti());
        permissionCacheService.cachePermissions(userId, permissions);
        dauService.record(userId);

        response.addCookie(createRefreshCookie(refreshInfo.token()));

        // 소셜 로그인은 302 리다이렉트로만 응답이 끝나 accessToken을 응답 바디에 담을 수 없음
        // → 프론트 콜백 페이지(/oauth2/redirect)에 쿼리 파라미터로 전달, 프론트가 localStorage에 저장 후 URL에서 제거
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("userId", userId)
                .queryParam("role", role)
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    private Cookie createRefreshCookie(String value) {
        int refreshTtlSeconds = jwtProvider.getConfig().refreshTtlDays() * 24 * 60 * 60;
        return refreshCookieProvider.create(value, refreshTtlSeconds);
    }
}
