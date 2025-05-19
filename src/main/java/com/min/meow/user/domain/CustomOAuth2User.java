package com.min.meow.user.domain;

import com.min.meow.user.domain.dto.UserDto;
import com.min.meow.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    //private final UserDto userDto;
    private final User user;

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Collection<GrantedAuthority> collection = new ArrayList<>();

        collection.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {

                return user.getRole().name();
            }
        });
        return collection;
    }

    @Override
    public String getName() {
        return user.getName();
    }

    public String getLoginId(){

        return user.getLoginId();
    }
}
