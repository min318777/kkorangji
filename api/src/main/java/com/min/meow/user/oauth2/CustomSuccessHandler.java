package com.min.meow.user.oauth2;

import com.min.meow.global.Token;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.domain.CustomOAuth2User;
import com.min.meow.user.entity.RefreshToken;
import com.min.meow.user.entity.User;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.repository.RefreshTokenRepository;
import com.min.meow.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;


    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    //private final OAuth2AuthorizedClientService authorizedClientService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 발급받은 access토큰을 통해 구글 리소스서버에서 회원정보를 가져온 후 서비스의 jwt발급 및 프론트엔드에 전달 로직
        // OAuth2User
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        String loginId = customUserDetails.getLoginId();

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        Long userId = user.getId();

        String refreshToken = jwtUtil.createRefreshToken(userId, REFRESH_TOKEN_EXPIRATION,Token.REFRESH_TOKEN);

        addRefreshToken(loginId, userId, refreshToken);

        response.addCookie(createCookie("refresh", refreshToken));
        response.sendRedirect("http://localhost:3000/"); // /reissue를 통해 리프레쉬토큰을 가지고 액세스토큰을 헤더에 담아 재응답 받는다.



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
        cookie.setMaxAge(60*60*60);
        //cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }

    private void addRefreshToken(String loginId, Long userId, String refresh) {

        RefreshToken refreshToken = RefreshToken.builder()
                .loginId(loginId)
                .refreshToken(refresh)
                .userId(userId)
                .expiration(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_EXPIRATION / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
    }
}
