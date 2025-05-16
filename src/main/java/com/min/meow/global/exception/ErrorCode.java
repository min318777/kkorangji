package com.min.meow.global.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    
    // 게시글 예외
    NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않은 게시글입니다."),
    
    // 회원 예외
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    ALREADY_EXISTING_USER(HttpStatus.CONFLICT, "중복된 아이디가 존재합니다."),
    ALREADY_EXISTING_EMAIL(HttpStatus.CONFLICT, "동일한 이메일의 계정이 존재합니다." ),


    // jwt 예외
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 access 토큰입니다.");



    ;

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message){
        this.status = status;
        this.message = message;
    }
}
