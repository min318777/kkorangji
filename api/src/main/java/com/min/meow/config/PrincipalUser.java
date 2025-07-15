package com.min.meow.config;

import com.min.meow.user.entity.User;

public interface PrincipalUser extends org.springframework.security.core.userdetails.UserDetails{
    String getLoginId();
    User getUser();
}
