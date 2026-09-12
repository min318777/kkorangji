package com.min.meow.common.exception;


import lombok.Getter;

import java.util.Map;

@Getter
public class CustomException extends RuntimeException{
    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public CustomException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " : " + detail);
        this.errorCode = errorCode;
        this.details = null;
    }

    public CustomException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getMessage() + " : " + details);
        this.errorCode = errorCode;
        this.details = details;
    }

    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }
}
