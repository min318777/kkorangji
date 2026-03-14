package com.min.meow.global.exception;

import com.min.meow.global.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외 처리 (의도된 비즈니스 예외 → warn)
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("비즈니스 예외 발생 [{}]: {}", errorCode.name(), e.getMessage());
        return createErrorResponse(errorCode.getStatus(), errorCode.getMessage());
    }

    /**
     * @PreAuthorize 권한 거부 예외 처리.
     * AuthorizationDeniedException은 Spring Security 6.x에서 AccessDeniedException을 대체한다.
     * GlobalExceptionHandler에 핸들러가 없으면 Exception.class에 잡혀 500이 반환되므로
     * 명시적으로 403 Forbidden을 반환하도록 처리한다.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.warn("권한 없는 접근 시도: {}", e.getMessage());
        return createErrorResponse(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    // Validation 예외 처리 (클라이언트 입력 오류 → 실패한 모든 필드 목록 반환)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 실패한 모든 필드를 FieldErrorDetail 리스트로 변환
        List<FieldErrorDetail> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("입력값 검증 실패 - 필드 수: {}, 에러: {}", fieldErrors.size(), fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다", fieldErrors));
    }

    /**
     * SSE 타임아웃 예외 처리
     * SSE 연결이 타임아웃되면 발생하는 예외
     * 정상적인 SSE 동작의 일부이므로 에러 로그를 남기지 않고 조용히 처리
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        // SSE 타임아웃은 정상적인 동작이므로 debug 레벨로 로깅
        log.debug("SSE 연결 타임아웃 - 클라이언트 재연결 필요");
        // 204 No Content 반환 (응답 본문 없음)
        return ResponseEntity.noContent().build();
    }

    // 기타 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unexpected error 발생", e);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다.");
    }

    // ApiResponse.fail()을 사용한 통합 에러 응답 생성
    private ResponseEntity<ApiResponse<?>> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(status, message));
    }
}