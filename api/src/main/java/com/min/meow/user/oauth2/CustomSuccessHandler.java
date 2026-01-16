package com.min.meow.user.oauth2;

import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.entity.User;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // Refresh Token: 14일
    private static final long REFRESH_TOKEN_EXPIRATION = 14 * 24 * 60 * 60 * 1000L;

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    //private final OAuth2AuthorizedClientService authorizedClientService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 발급받은 구굴의 access토큰을 통해 구글 리소스서버에서 회원정보를 가져온 후 서비스의 jwt발급 및 프론트엔드에 전달 로직
        // OAuth2User
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        String loginId = customUserDetails.getLoginId();

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        Long userId = user.getId();

        String refreshToken = jwtUtil.createRefreshToken(userId, Token.REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION);

        // Redis에 Refresh Token 저장
        refreshTokenService.save(userId, refreshToken);

        response.addCookie(createCookie("refresh", refreshToken));
        response.sendRedirect("http://localhost:3000/");

        /*구글 인증서버에서 ID토큰과 access토큰을 받은 후 ID토큰을 직접 파싱해서 회원가입+로그인 처리 로직
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );
         */
    }

    private Cookie createCookie(String key, String value){
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(14 * 24 * 60 * 60); // 14일
        //cookie.setSecure(true); -> https 통신시 필요
        cookie.setPath("/");
        cookie.setHttpOnly(true); // XSS 공격 방어
        return cookie;
    }
}
