package com.min.meow.user.service;


import com.min.meow.global.Role;
import com.min.meow.user.domain.dto.*;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

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

        User existUser = userRepository.findByLoginId(loginId);

        if(existUser == null){
            User user = User.builder()
                    .loginId(loginId)
                    .email(oAuth2Response.getEmail())
                    .name(oAuth2Response.getName())
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(user);

            UserDto userDto = new UserDto();
            userDto.setRole("ROLE_USER");
            userDto.setName(oAuth2Response.getName());
            userDto.setLoginId(loginId);

            return new CustomOAuth2User(userDto);
        }else{

            existUser.setEmail(oAuth2Response.getEmail());
            existUser.setName(oAuth2Response.getName());

            userRepository.save(existUser);

            UserDto userDto = new UserDto();
            userDto.setRole("ROLE_USER");
            userDto.setName(oAuth2Response.getName());
            userDto.setLoginId(existUser.getLoginId());

            return new CustomOAuth2User(userDto);

        }
    }
}
