package com.min.meow.user.service;

import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.dto.CustomUserDetails;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // - 소프트 삭제된 사용자는 시스템에 존재하지만 로그인할 수 없음
        if (user.isWithdrawn()) {
            log.warn("탈퇴한 사용자 로그인 시도 - userId: {}", user.getId());
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }
        return new CustomUserDetails(user);
    }
}
