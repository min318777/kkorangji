package com.min.meow.security.oauth2;


import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.user.dto.response.KakaoResponse;
import com.min.meow.user.dto.response.OAuth2Response;
import com.min.meow.user.entity.Role;
import com.min.meow.user.entity.User;
import com.min.meow.user.entity.UserRole;
import com.min.meow.user.repository.RoleRepository;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.repository.UserRoleRepository;
import com.min.meow.user.util.NicknameGenerator;
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
    private final NicknameGenerator nicknameGenerator;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 어느 소셜 provider에서 온 응답인지 구분
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2Response oAuth2Response;
        if (registrationId.equals("kakao")) {
            oAuth2Response = new KakaoResponse(oAuth2User.getAttributes());
        } else {
            // 카카오 외 provider는 현재 범위 밖 (google/naver 응답 파서는 남겨뒀지만 아직 미연결)
            throw new OAuth2AuthenticationException("지원하지 않는 로그인 provider입니다: " + registrationId);
        }

        String provider = oAuth2Response.getProvider();
        String socialId = oAuth2Response.getProviderId();

        Optional<User> existingUser = userRepository.findByProviderAndSocialId(provider, socialId);

        if (existingUser.isEmpty()) {
            // 최초 로그인 → 그 자리에서 자동 가입 (loginId/email은 소셜 계정 전용이라 null)
            User user = User.builder()
                    .provider(provider)
                    .socialId(socialId)
                    .password(null)
                    .nickname(nicknameGenerator.generate())
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
        } else {
            User user = existingUser.get();
            user.updateLastLogin();
            userRepository.save(user);

            return new CustomOAuth2User(user);
        }
    }


}
