package com.min.meow.user.controller;


import com.min.meow.global.exception.ApiResponse;
import com.min.meow.global.exception.ErrorResponse;
import com.min.meow.user.domain.dto.JoinDto;
import com.min.meow.user.domain.dto.LoginDto;
import com.min.meow.user.domain.request.JoinRequest;
import com.min.meow.user.domain.request.LoginRequest;
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
    public ResponseEntity<ApiResponse<JoinDto>> join(@RequestBody @Valid JoinRequest joinRequest){

        JoinDto joinDto = userService.join(joinRequest);

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "회원가입 성공", joinDto));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginDto>> login(@RequestBody @Valid LoginRequest loginRequest){
        LoginDto loginDto = userService.login(loginRequest);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "로그인 성공", loginDto));
    }
}
