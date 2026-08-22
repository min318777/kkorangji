package com.min.meow.user.util;

import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 소셜 로그인 최초 가입 시 사용할 임시 닉네임 자동 생성
 * JoinRequest 닉네임 검증 규칙(^[가-힣a-zA-Z0-9]+$, 2~10자)을 만족하는 형식으로 생성
 * 사용자는 나중에 마이페이지에서 자유롭게 변경 가능 (온보딩 강제 관문 없음)
 */
@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private final UserRepository userRepository;

    private static final String[] ADJECTIVES = {
            "냥냥", "귀여운", "포근한", "다정한", "든든한", "명랑한", "느긋한", "씩씩한"
    };
    private static final String[] NOUNS = {
            "집사", "고양이", "냥이", "발바닥", "수염", "꼬랑지"
    };
    private static final int MAX_RETRY = 10;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        for (int i = 0; i < MAX_RETRY; i++) {
            String candidate = ADJECTIVES[random.nextInt(ADJECTIVES.length)]
                    + NOUNS[random.nextInt(NOUNS.length)]
                    + (1000 + random.nextInt(9000)); // 4자리 랜덤 숫자
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        // 그래도 계속 충돌하면 UUID 조각으로 유일성 보장
        return "냥이" + java.util.UUID.randomUUID().toString().substring(0, 6);
    }
}
