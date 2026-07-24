package com.min.meow.security.userdetails;

import com.min.meow.common.PrincipalUser;
import com.min.meow.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails, PrincipalUser {

    private final Long userId;
    private final String role;
    private final List<String> permissions;
    private final String password;  // 로그인 시 BCrypt 비교용, JWT 인증 시엔 null

    // JWT 필터용 — password 불필요
    public CustomUserDetails(Long userId, String role, List<String> permissions) {
        this(userId, role, permissions, null);
    }

    // 로그인용 — password 포함
    public CustomUserDetails(Long userId, String role, List<String> permissions, String password) {
        this.userId = userId;
        this.role = role != null ? role : "ROLE_USER";
        this.permissions = permissions != null ? new ArrayList<>(permissions) : List.of();
        this.password = password;
    }

    public static CustomUserDetails from(User user) {
        // 역할이 여럿이어도 첫 번째 하나만 대표 role로 사용 (JWT 구조와 일치)
        String role = user.getRoleNames().stream()
                .findFirst()
                .orElse("ROLE_USER");
        List<String> permissions = new ArrayList<>(user.getAllPermissionCodes());
        // 로그인 시 Spring Security BCrypt 비교를 위해 password 포함
        return new CustomUserDetails(user.getId(), role, permissions, user.getPassword());
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // Role 추가 (hasRole("ADMIN") 호환)
        authorities.add(new SimpleGrantedAuthority(role));
        // Permission 추가 (hasAuthority("post:write") 사용)
        for (String perm : permissions) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

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