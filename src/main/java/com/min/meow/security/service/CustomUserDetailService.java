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

/**
 * Spring Security UserDetailsService 구현체
 * 로그인 시 사용자 정보를 조회하고 인증에 필요한 UserDetails 객체를 반환합니다.
 * 탈퇴한 사용자는 로그인이 차단됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * loginId로 사용자 정보를 조회하여 UserDetails 반환
     * @param loginId 로그인 시도하는 사용자의 loginId
     * @return UserDetails 객체 (CustomUserDetails)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없거나 탈퇴한 사용자인 경우
     */
    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        // UsernameNotFoundException을 던져야 Spring Security가 InternalAuthenticationServiceException으로 래핑하지 않음
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
