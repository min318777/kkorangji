package com.min.meow.security.dto;

import com.min.meow.global.PrincipalUser;
import com.min.meow.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails, PrincipalUser {

    private final User user;

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
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getLoginId();
    }

    @Override
    public String getLoginId() {
        return user.getLoginId();
    }

    @Override
    public Long getUserId() { return user.getId(); }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
