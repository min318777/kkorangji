package com.min.meow.user.controller;


import com.min.meow.global.ResponseDto;
import com.min.meow.user.domain.JoinDto;
import com.min.meow.user.domain.JoinRequest;
import com.min.meow.user.service.JoinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/join")
public class JoinController {

    private final JoinService joinService;

    @PostMapping
    public ResponseEntity<?> join(@RequestBody @Valid JoinRequest joinRequest){

        JoinDto joinDto = joinService.join(joinRequest);

        return ResponseEntity.ok(new ResponseDto<>(true, "회원가입 성공", joinDto));
    }
}
