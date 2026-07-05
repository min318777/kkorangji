package com.min.meow.common;

import org.springframework.security.core.userdetails.UserDetails;

public interface PrincipalUser extends UserDetails {
    Long getUserId();
}
