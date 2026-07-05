package com.min.meow.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext 기반 권한 확인 유틸리티
 * 서비스 레이어에서 DI 없이 현재 사용자의 권한을 체크할 수 있다.
 * 사용 예시:
 * - SecurityUtil.hasAuthority("post:delete") → 관리자의 타인 게시글 삭제 허용
 * - SecurityUtil.hasAuthority("comment:delete") → 관리자의 타인 댓글 삭제 허용
 */
public class SecurityUtil {

    private SecurityUtil() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }

    /**
     * 현재 인증된 사용자가 특정 authority를 보유하고 있는지 확인
     * @param authority 확인할 권한 코드 (예: "post:delete", "comment:delete")
     * @return 해당 권한 보유 여부
     */
    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
