package com.min.meow.user.oauth2;

import com.min.meow.global.Token;
import com.min.meow.user.domain.CustomOAuth2User;
import com.min.meow.user.entity.RefreshEntity;
import com.min.meow.user.jwt.JwtUtil;
import com.min.meow.user.repository.RefreshEntityRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshEntityRepository refreshEntityRepository;

    //private final OAuth2AuthorizedClientService authorizedClientService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 발급받은 access토큰을 통해 구글 리소스서버에서 회원정보를 가져온 후 서비스의 jwt발급 및 프론트엔드에 전달 로직
        // OAuth2User
        CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();
        String loginId = customUserDetails.getLoginId();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();



        String accessToken = jwtUtil.createJwt(Token.ACCESS_TOKEN, loginId, role, 600000L);
        String refreshToken = jwtUtil.createJwt(Token.REFRESH_TOKEN, loginId, role, 86400000L);

        addRefreshEntity(loginId, refreshToken, 86400000L);

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

    private void addRefreshEntity(String loginId, String refresh, Long expiredMs) {

        Date date = new Date(System.currentTimeMillis() + expiredMs);
        RefreshEntity refreshEntity = new RefreshEntity();
        refreshEntity.setLoginId(loginId);
        refreshEntity.setRefreshToken(refresh);
        refreshEntity.setExpiration(date.toString());

        refreshEntityRepository.save(refreshEntity);
    }
}
