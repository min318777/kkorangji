package com.min.meow.user.service.impl;

import com.min.meow.global.Role;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.user.dto.reponse.JoinResponse;
import com.min.meow.user.dto.reponse.LoginResponse;
import com.min.meow.user.dto.request.JoinRequest;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.RefreshTokenRepository;
import com.min.meow.user.repository.UserRepository;
import com.min.meow.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
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

    /**
     * 회원 탈퇴 처리
     *
     * 1. 사용자 존재 여부 확인
     * 2. 이미 탈퇴한 사용자인지 확인
     * 3. 리프레시 토큰 삭제 (로그인 세션 무효화)
     * 4. 개인정보 비식별화 및 소프트 삭제 처리
     *
     * 소프트 삭제 방식을 사용하여:
     * - 게시글, 댓글 등 기존 데이터의 외래키 참조 무결성 유지
     * - 탈퇴 후에도 게시글은 "탈퇴한 사용자"로 표시되어 조회 가능
     * - 개인정보는 비식별화되어 개인정보보호법 준수
     *
     * @param loginId 탈퇴할 사용자의 로그인 ID
     */
    @Override
    @Transactional
    public void withdraw(String loginId) {
        // 1. 사용자 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 2. 이미 탈퇴한 사용자인지 확인
        if (user.isWithdrawn()) {
            throw new CustomException(ErrorCode.ALREADY_WITHDRAWN_USER);
        }

        // 3. 리프레시 토큰 삭제 - 모든 디바이스에서 로그아웃 처리
        refreshTokenRepository.deleteByLoginId(loginId);
        log.info("회원 탈퇴 처리 - loginId: {}, 리프레시 토큰 삭제 완료", loginId);

        // 4. 개인정보 비식별화 및 소프트 삭제 처리
        user.withdraw();
        userRepository.save(user);

        log.info("회원 탈퇴 완료 - userId: {}, 비식별화된 loginId: {}", user.getId(), user.getLoginId());
    }
}
