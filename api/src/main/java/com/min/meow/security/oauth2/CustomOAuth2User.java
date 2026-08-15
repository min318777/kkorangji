package com.min.meow.security.oauth2;

import com.min.meow.common.PrincipalUser;
import com.min.meow.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User, PrincipalUser {

    private final User user;

    @Override
    public Map<String, Object> getAttributes() {
        // null 대신 빈 맵 반환 — Spring 내부에서 getAttributes()를 참조하는 경로가 있을 수 있어 안전하게 처리
        return Map.of();
    }

    @Override
    public Long getUserId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Role 추가 (hasRole("ADMIN") 호환)
        for (String roleName : user.getRoleNames()) {
            authorities.add(new SimpleGrantedAuthority(roleName));
        }

        // Permission 추가 (hasAuthority("post:write") 사용)
        for (String code : user.getAllPermissionCodes()) {
            authorities.add(new SimpleGrantedAuthority(code));
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        // AbstractAuthenticationToken.getName()은 principal이 UserDetails면 getUsername()을 우선 사용함
        // (getName()보다 먼저 체크됨) — 소셜 로그인 유저는 loginId가 null이라 principalName이 빈 값이 되어
        // "principalName cannot be empty" 예외가 발생했음. loginId가 없으면 provider+socialId로 대체
        String loginId = user.getLoginId();
        if (loginId != null && !loginId.isBlank()) {
            return loginId;
        }
        return user.getProvider() + "_" + user.getSocialId();
    }

    @Override
    public String getName() {
        return getUsername();
    }


}
