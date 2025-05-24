package com.min.meow.user.config;

import com.min.meow.user.entity.User;

public interface PrincipalUser {
    String getLoginId();
    User getUser();
}
