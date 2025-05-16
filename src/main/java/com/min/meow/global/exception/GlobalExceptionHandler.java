package com.min.meow.global.exception;

import com.min.meow.global.ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ResponseDto<?>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("CustomException 발생: {}", e.getMessage());
        return createErrorResponse(errorCode.getStatus(), errorCode.getMessage());
        //return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseDto<>(false, e.getMessage(), null));
    }

    // Validation 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        log.error("Validation error 발생: {}", errorMessage);
        return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    // 기타 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto<?>> handleException(Exception e) {
        log.error("Unexpected error 발생", e);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다.");
    }

    private ResponseEntity<ResponseDto<?>> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(new ResponseDto<>(false, message, null));
    }
}
