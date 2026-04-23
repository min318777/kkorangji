package com.min.meow.global;

import org.springframework.security.core.userdetails.UserDetails;

public interface PrincipalUser extends UserDetails {
    Long getUserId();
}
