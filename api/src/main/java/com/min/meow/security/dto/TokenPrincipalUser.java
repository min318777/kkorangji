package com.min.meow.security.dto;

import com.min.meow.global.PrincipalUser;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * DB 조회 없이 JWT Claims로부터 생성되는 경량 PrincipalUser 구현체.
 *
 * <p>v2 인증 방식에서 사용되며, Access Token에 포함된 userId, role, loginId만으로
 * SecurityContext 인증 정보를 구성한다. DB 조회를 제거하여 인증 성능을 최적화.</p>
 *
 * <p>컨트롤러에서 User 엔티티가 필요한 경우 별도로 DB 조회가 필요하다.</p>
 */
public class TokenPrincipalUser implements PrincipalUser {

    private final Long userId;
    private final String role;
    private final String loginId;

    public TokenPrincipalUser(Long userId, String role, String loginId) {
        this.userId = userId;
        this.role = role;
        this.loginId = loginId;
    }

    @Override
    public String getLoginId() {
        return loginId;
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> role);
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return loginId;
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
