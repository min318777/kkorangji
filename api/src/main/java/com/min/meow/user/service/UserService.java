package com.min.meow.user.service;


import com.min.meow.user.domain.dto.JoinDto;
import com.min.meow.user.domain.dto.LoginDto;
import com.min.meow.user.domain.request.JoinRequest;
import com.min.meow.user.domain.request.LoginRequest;

public interface UserService {

    LoginDto login(LoginRequest loginRequest);
    JoinDto join(JoinRequest joinRequest);



}
