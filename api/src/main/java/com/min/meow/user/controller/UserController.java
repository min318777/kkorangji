package com.min.meow.user.controller;


import com.min.meow.global.ApiResponse;
import com.min.meow.user.dto.reponse.JoinResponse;
import com.min.meow.user.dto.reponse.LoginResponse;
import com.min.meow.user.dto.request.JoinRequest;
import com.min.meow.user.dto.request.LoginRequest;
import com.min.meow.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<JoinResponse>> join(@RequestBody @Valid JoinRequest joinRequest){

        JoinResponse joinResponse = userService.join(joinRequest);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "회원가입 성공", joinResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest loginRequest){
        LoginResponse loginResponse = userService.login(loginRequest);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "로그인 성공", loginResponse));
    }
}
