package com.min.meow.user.service;


import com.min.meow.user.domain.LoginDto;
import com.min.meow.user.domain.LoginRequest;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;

    public LoginDto login(LoginRequest loginRequest){

        User user = userRepository.findByLoginId(loginRequest.getLoginId());

        return LoginDto.builder()
                .loginId(user.getLoginId())
                .build();
    }
}
