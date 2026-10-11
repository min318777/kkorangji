package com.min.meow.security.service;

import com.min.meow.security.userdetails.CustomUserDetails;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Spring Security UserDetailsService 구현체 — 로그인 시 사용자 조회 후 UserDetails 반환
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + loginId));

        if (user.isWithdrawn()) {
            log.warn("탈퇴한 사용자 로그인 시도 - userId: {}", user.getId());
            // DisabledException: Spring Security 표준 예외, unsuccessfulAuthentication에서 처리됨
            throw new DisabledException("탈퇴한 사용자입니다.");
        }
        return CustomUserDetails.from(user);
    }
}
