package com.min.meow.global.exception;


import lombok.Getter;

@Getter
public enum ErrorCode {

    NOT_FOUND(404, "");

    private final int status;
    private final String message;

    ErrorCode(int status, String message){
        this.status = status;
        this.message = message;
    }
}
