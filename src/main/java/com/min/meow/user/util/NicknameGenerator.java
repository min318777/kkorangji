package com.min.meow.user.util;

import com.min.meow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

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
