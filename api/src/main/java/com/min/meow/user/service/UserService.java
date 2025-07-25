package com.min.meow.user.service;


import com.min.meow.user.domain.reponse.JoinResponse;
import com.min.meow.user.domain.reponse.LoginResponse;
import com.min.meow.user.domain.request.JoinRequest;
import com.min.meow.user.domain.request.LoginRequest;

public interface UserService {

    LoginResponse login(LoginRequest loginRequest);
    JoinResponse join(JoinRequest joinRequest);



}
