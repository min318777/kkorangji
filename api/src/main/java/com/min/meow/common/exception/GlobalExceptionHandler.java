package com.min.meow.common.exception;

import com.min.meow.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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
        return createErrorResponse(errorCode.getStatus(), errorCode.name(), errorCode.getMessage());
    }

    /**
     * @PreAuthorize 권한 거부 예외 처리.
     * AuthorizationDeniedException은 Spring Security 6.x에서 AccessDeniedException을 대체한다.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.warn("권한 없는 접근 시도: {}", e.getMessage());
        return createErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.");
    }

    // Validation 예외 처리 — 실패한 모든 필드 목록 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldErrorDetail> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("입력값 검증 실패 - 필드 수: {}, 에러: {}", fieldErrors.size(), fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값이 올바르지 않습니다", fieldErrors));
    }

    // IllegalArgumentException 처리 — 잘못된 파일 형식 등 클라이언트 입력 오류 (기존에는 500 반환됨)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 입력값: {}", e.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", e.getMessage());
    }

    /**
     * SSE 타임아웃 예외 처리 — 정상적인 SSE 동작이므로 조용히 처리
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.debug("SSE 연결 타임아웃 - 클라이언트 재연결 필요");
        return ResponseEntity.noContent().build();
    }

    // 필수 쿼리 파라미터 누락 — 400 (예: /mypage/posts에서 type 없이 요청)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락: {}", e.getParameterName());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "필수 파라미터가 없습니다: " + e.getParameterName());
    }

    // 잘못된 HTTP 메서드 — 405 (예: POST로 요청해야 하는데 GET으로)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 HTTP 메서드: {}", e.getMethod());
        return createErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "지원하지 않는 HTTP 메서드입니다: " + e.getMethod());
    }

    // 잘못된 Content-Type — 415 (예: application/json 필요한데 없이 요청)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 Content-Type: {}", e.getContentType());
        return createErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "지원하지 않는 Content-Type입니다. application/json을 사용하세요.");
    }

    // Request Body 파싱 실패 — 400 (JSON 형식 오류)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Request Body 파싱 실패: {}", e.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "요청 본문을 읽을 수 없습니다. JSON 형식을 확인하세요.");
    }

    // 예상치 못한 예외 처리 — 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("예상치 못한 오류 발생", e);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "알 수 없는 오류가 발생했습니다.");
    }

    private ResponseEntity<ApiResponse<?>> createErrorResponse(HttpStatus status, String errorCode, String message) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(status, errorCode, message));
    }
}
