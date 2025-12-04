package com.min.meow.user.service;


import com.min.meow.user.dto.reponse.JoinResponse;
import com.min.meow.user.dto.reponse.LoginResponse;
import com.min.meow.user.dto.request.JoinRequest;
import com.min.meow.user.dto.request.LoginRequest;

public interface UserService {

    LoginResponse login(LoginRequest loginRequest);
    JoinResponse join(JoinRequest joinRequest);



}
