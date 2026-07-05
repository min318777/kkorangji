package com.min.meow.security.oauth2;

import com.min.meow.common.PrincipalUser;
import com.min.meow.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User, PrincipalUser {

    private final User user;

    @Override
    public Map<String, Object> getAttributes() {
        return null;
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
        return user.getLoginId();
    }

    @Override
    public String getName() {
        return user.getNickname();
    }


}
