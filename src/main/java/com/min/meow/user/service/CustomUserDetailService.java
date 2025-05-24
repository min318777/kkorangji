package com.min.meow.user.service;

import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.domain.CustomUserDetails;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {

        User user = userRepository.findByLoginId(loginId);

        if(user == null){
            throw new CustomException(ErrorCode.NOT_FOUND_USER);

            //throw new UsernameNotFoundException("해당 유저가 없습니다"); => Custom예외가 Authentication예외를 상속하지않기때문에 로그인실패핸들러에서 잡히지 않을수있따.
        }
        return new CustomUserDetails(user);
    }
}
