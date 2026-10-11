package com.min.meow.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// SecurityContext 기반 권한 확인 유틸리티 — 서비스 레이어에서 DI 없이 현재 사용자 권한 체크
public class SecurityUtil {

    private SecurityUtil() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }

    // 현재 인증된 사용자가 특정 authority를 보유하고 있는지 확인
    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
