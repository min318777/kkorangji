package com.min.meow.user.service;


import com.min.meow.global.Role;
import com.min.meow.user.oauth2.CustomOAuth2User;
import com.min.meow.user.domain.reponse.GoogleResponse;
import com.min.meow.user.domain.reponse.NaverResponse;
import com.min.meow.user.domain.reponse.OAuth2Response;
import com.min.meow.user.domain.UserDto;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

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

        Optional<User> existUser = userRepository.findByLoginId(loginId);

        if(existUser.isEmpty()){
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

            existUser.get().setEmail(oAuth2Response.getEmail());
            existUser.get().setName(oAuth2Response.getName());
            existUser.get().setLastLoginAt(LocalDateTime.now());
            userRepository.save(existUser.get());

            UserDto userDto = new UserDto();
            userDto.setRole("ROLE_USER");
            userDto.setName(oAuth2Response.getName());
            userDto.setLoginId(existUser.get().getLoginId());

            return new CustomOAuth2User(existUser.get());
        }
    }


}
