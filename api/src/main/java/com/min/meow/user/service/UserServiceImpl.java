package com.min.meow.user.service;

import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.domain.dto.JoinDto;
import com.min.meow.user.domain.dto.LoginDto;
import com.min.meow.user.domain.request.JoinRequest;
import com.min.meow.user.domain.request.LoginRequest;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public LoginDto login(LoginRequest loginRequest){
        User user = userRepository.findByLoginId(loginRequest.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.UNREGISTERED_USER));

        return LoginDto.builder()
                .loginId(user.getLoginId())
                .rememberMe(loginRequest.isRememberMe())
                .build();
    }

    public JoinDto join(JoinRequest joinRequest){

        if (userRepository.existsByEmail(joinRequest.getEmail())) {
            throw new CustomException(ErrorCode.ALREADY_EXISTING_EMAIL);
        }
        if (userRepository.existsByLoginId(joinRequest.getLoginId())){
            throw new CustomException(ErrorCode.ALREADY_EXISTING_USER);
        }

        String encodedPassword = bCryptPasswordEncoder.encode(joinRequest.getPassword());
        User user = User.convertToEntity(joinRequest);
        user.setPassword(encodedPassword);
        user.setRole(joinRequest.getRole());
        userRepository.save(user);

        return JoinDto.convertToDto(user);
    }
}
