package com.min.meow.user.service;


import com.min.meow.global.Role;
import com.min.meow.user.domain.CustomOAuth2User;
import com.min.meow.user.domain.dto.GoogleResponse;
import com.min.meow.user.domain.dto.NaverResponse;
import com.min.meow.user.domain.dto.OAuth2Response;
import com.min.meow.user.domain.dto.UserDto;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    /* 구글방식
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 기본적으로 OAuth2User를 로드합니다
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 구글 로그인을 할 경우에는 OIDCUser로 캐스팅을 시도
        if (userRequest.getClientRegistration().getRegistrationId().equals("google")) {
            OidcUser oidcUser = (OidcUser) oAuth2User;

            // ID 토큰을 통해 구글 사용자 정보 추출
            String email = oidcUser.getEmail();
            String loginId = oidcUser.getSubject(); // 구글 고유 ID
            String name = oidcUser.getFullName(); // 구글에서 제공하는 사용자 이름

            // 사용자 DB에서 이메일 중복 확인
            User emailConflictUser = userRepository.findByEmail(email);
            if (emailConflictUser != null && !emailConflictUser.getLoginId().equals(loginId)) {
                throw new OAuth2AuthenticationException("이미 동일한 이메일로 가입된 계정이 존재합니다.");
            }

            // 로그인 시 회원가입 또는 로그인 처리
            User existUser = userRepository.findByLoginId(loginId);
            if (existUser == null) {
                // 새 사용자 등록
                User user = User.builder()
                        .loginId(loginId)
                        .email(email)
                        .password(null)
                        .name(name)
                        .role(Role.ROLE_USER)
                        .isDelete(false)
                        .lastLoginAt(LocalDateTime.now())
                        .build();
                userRepository.save(user);
                return new CustomOAuth2User(user);
            } else {

                existUser.setEmail(email);
                existUser.setName(name);
                existUser.setLastLoginAt(LocalDateTime.now());
                userRepository.save(existUser);
                return new CustomOAuth2User(existUser);
            }
        }
        return super.loadUser(userRequest);

     */


    // 개발자유미 로직
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        System.out.println(oAuth2User);

        // 구글에서 온값인지, 네이버에서 온값인지 구분하는 id
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2Response oAuth2Response = null;

        if(registrationId.equals("naver")){
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        }else if(registrationId.equals("google")){
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        } else{
            return null;
        }

        String loginId = oAuth2Response.getProvider() + " " + oAuth2Response.getProviderId();
        String existEmail = oAuth2Response.getEmail();

        User emailConflictUser = userRepository.findByEmail(existEmail);
        if (emailConflictUser != null && !emailConflictUser.getLoginId().equals(loginId)) {
            throw new OAuth2AuthenticationException("이미 동일한 이메일로 가입된 계정이 존재합니다.");
            //throw new CustomException(ErrorCode.ALREADY_EXISTING_EMAIL);
        }

        User existUser = userRepository.findByLoginId(loginId);

        if(existUser == null){
            User user = User.builder()
                    .loginId(loginId)
                    .email(oAuth2Response.getEmail())
                    .password(null)
                    .name(oAuth2Response.getName())
                    .role(Role.ROLE_USER)
                    .isDelete(false)
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);

            UserDto userDto = new UserDto();
            userDto.setRole("ROLE_USER");
            userDto.setName(oAuth2Response.getName());
            userDto.setLoginId(loginId);

            return new CustomOAuth2User(user);
        }else{

            existUser.setEmail(oAuth2Response.getEmail());
            existUser.setName(oAuth2Response.getName());
            existUser.setLastLoginAt(LocalDateTime.now());
            userRepository.save(existUser);
            UserDto userDto = new UserDto();
            userDto.setRole("ROLE_USER");
            userDto.setName(oAuth2Response.getName());
            userDto.setLoginId(existUser.getLoginId());

            return new CustomOAuth2User(existUser);
        }
    }


}
