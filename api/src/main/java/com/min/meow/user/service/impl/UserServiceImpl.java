package com.min.meow.user.service.impl;

import com.min.meow.global.Role;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.dto.reponse.JoinResponse;
import com.min.meow.user.dto.reponse.LoginResponse;
import com.min.meow.user.dto.request.JoinRequest;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public LoginResponse login(LoginRequest loginRequest){
        User user = userRepository.findByLoginId(loginRequest.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.UNREGISTERED_USER));

        return LoginResponse.builder()
                .loginId(user.getLoginId())
                .rememberMe(loginRequest.isRememberMe())
                .build();
    }

    public JoinResponse join(JoinRequest joinRequest){

        if (userRepository.existsByEmail(joinRequest.getEmail())) {
            throw new CustomException(ErrorCode.ALREADY_EXISTING_EMAIL);
        }
        if (userRepository.existsByLoginId(joinRequest.getLoginId())){
            throw new CustomException(ErrorCode.ALREADY_EXISTING_USER);
        }

        String encodedPassword = bCryptPasswordEncoder.encode(joinRequest.getPassword());

        // 엔티티 직접 생성 (toEntity 메서드 제거됨)
        User user = User.builder()
                .loginId(joinRequest.getLoginId())
                .password(encodedPassword)
                .name(joinRequest.getName())
                .email(joinRequest.getEmail())
                .role(joinRequest.getRole() != null ? joinRequest.getRole() : Role.ROLE_USER)
                .isDelete(false)
                .build();

        userRepository.save(user);

        return JoinResponse.convertToDto(user);
    }
}
