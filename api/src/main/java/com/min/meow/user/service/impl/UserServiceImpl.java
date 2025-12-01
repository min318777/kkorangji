package com.min.meow.user.service.impl;

import com.min.meow.global.Role;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.domain.reponse.JoinResponse;
import com.min.meow.user.domain.reponse.LoginResponse;
import com.min.meow.user.domain.request.JoinRequest;
import com.min.meow.user.domain.request.LoginRequest;
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
        User user = User.toEntity(joinRequest);
        user.setPassword(encodedPassword);
        if(joinRequest.getRole() == null){
            user.setRole(Role.ROLE_USER);
        }else{
            user.setRole(joinRequest.getRole());
        }
        userRepository.save(user);

        return JoinResponse.convertToDto(user);
    }
}
