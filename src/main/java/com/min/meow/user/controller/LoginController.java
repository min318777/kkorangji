package com.min.meow.user.controller;


import com.min.meow.global.ResponseDto;
import com.min.meow.user.domain.LoginDto;
import com.min.meow.user.domain.LoginRequest;
import com.min.meow.user.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/login")
public class LoginController {

    private final LoginService loginService;
    @PostMapping
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest){


        LoginDto loginDto = loginService.login(loginRequest);
        return ResponseEntity.ok(new ResponseDto<>(true, "로그인 성공", loginDto));
    }
}
