package com.min.meow.config;

import com.min.meow.user.entity.User;

public interface PrincipalUser {
    String getLoginId();
    User getUser();
}
