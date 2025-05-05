package com.min.meow.global.exception;


import lombok.Getter;

@Getter
public enum ErrorCode {

    NOT_FOUND(404, "존재하지 않은 게시글입니다."),
    NOT_FOUND_USER(404, "존재하지 않는 회원입니다."),
    ALREADY_EXISTING_USER(404, "중복된 아이디가 존재합니다.")

    ;

    private final int status;
    private final String message;

    ErrorCode(int status, String message){
        this.status = status;
        this.message = message;
    }
}
