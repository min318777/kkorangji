package com.min.meow.security.oauth2;


import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.dto.reponse.GoogleResponse;
import com.min.meow.user.dto.reponse.NaverResponse;
import com.min.meow.user.dto.reponse.OAuth2Response;
import com.min.meow.user.entity.Role;
import com.min.meow.user.entity.User;
import com.min.meow.user.entity.UserRole;
import com.min.meow.user.repository.RoleRepository;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.repository.UserRoleRepository;
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
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

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
        }

        Optional<User> existUser = userRepository.findByLoginId(loginId);

        if(existUser.isEmpty()){
            User user = User.builder()
                    .loginId(loginId)
                    .email(oAuth2Response.getEmail())
                    .password(null)
                    .name(oAuth2Response.getName())
                    .isDelete(false)
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);

            // 기본 역할(ROLE_USER) 부여
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ROLE));
            UserRole newUserRole = new UserRole(user, userRole);
            userRoleRepository.save(newUserRole);
            user.getUserRoles().add(newUserRole);

            return new CustomOAuth2User(user);
        }else{

            existUser.get().setEmail(oAuth2Response.getEmail());
            existUser.get().setName(oAuth2Response.getName());
            existUser.get().setLastLoginAt(LocalDateTime.now());
            userRepository.save(existUser.get());

            return new CustomOAuth2User(existUser.get());
        }
    }


}
