package com.min.meow.common.exception;

import com.min.meow.common.ApiResponse;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외 — 비즈니스 규칙 위반
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("비즈니스 예외 발생 [{}]: {}", errorCode.name(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getStatus(), errorCode.name(), errorCode.getMessage(),
                        MDC.get("requestId"), e.getDetails()));
    }

    // Spring 기본 예외 — 클라이언트 실수 (4xx)
    // 클라이언트 실수 — @Valid 검증 실패, 실패한 모든 필드 목록 반환
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<?>> handleBindException(BindException e) {
        List<FieldErrorDetail> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("입력값 검증 실패 - 필드 수: {}, 에러: {}", fieldErrors.size(), fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값이 올바르지 않습니다", fieldErrors));
    }

    // 클라이언트 실수 — Path variable 타입 불일치 (예: /posts/abc, id가 Long이어야 할 때)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 불일치: {} = {}", e.getName(), e.getValue());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER_TYPE",
                e.getName() + " 파라미터 형식이 올바르지 않습니다.");
    }

    // 클라이언트 실수 — 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락: {}", e.getParameterName());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "필수 파라미터가 없습니다: " + e.getParameterName());
    }

    // 클라이언트 실수 — Request Body JSON 형식 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Request Body 파싱 실패: {}", e.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "요청 본문을 읽을 수 없습니다. JSON 형식을 확인하세요.");
    }

    // 클라이언트 실수 — IllegalArgumentException (잘못된 파일 형식 등 입력값 오류)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 입력값: {}", e.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", e.getMessage());
    }

    // 클라이언트 실수 — 존재하지 않는 URL 경로 요청 (Spring Boot 3.x)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("존재하지 않는 경로: {}", e.getResourcePath());
        return createErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "요청한 경로가 존재하지 않습니다.");
    }

    // 클라이언트 실수 — 잘못된 HTTP 메서드 (예: GET으로 POST 엔드포인트 호출)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 HTTP 메서드: {}", e.getMethod());
        return createErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "지원하지 않는 HTTP 메서드입니다: " + e.getMethod());
    }

    // 클라이언트 실수 — 잘못된 Content-Type (예: application/json 필요한데 누락)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 Content-Type: {}", e.getContentType());
        return createErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "지원하지 않는 Content-Type입니다. application/json을 사용하세요.");
    }

    // Spring Security 예외 — 권한/인증 관련
    // 클라이언트 실수 — @PreAuthorize 권한 거부 (Spring Security 6.x에서 AccessDeniedException 대체)
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.warn("권한 없는 접근 시도: {}", e.getMessage());
        return createErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.");
    }

    // DB 예외 — 데이터 정합성 위반
    // 서버/클라이언트 혼합 — DB 유니크 제약 조건 위반 (동시 요청 발생)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DB 제약 조건 위반: {}", e.getMessage());
        return createErrorResponse(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "데이터 중복 또는 제약 조건 위반입니다.");
    }

    // 특수 케이스 — 정상 동작이지만 예외로 처리되는 경우
    // 정상 동작 — SSE 연결 타임아웃 (클라이언트가 재연결하면 됨, 에러 아님)
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.debug("SSE 연결 타임아웃 - 클라이언트 재연결 필요");
        return ResponseEntity.noContent().build();
    }

    // 글로벌 Exception 핸들러 — 예상 못한 모든 나머지 → 500
    // 서버 실수 — 예상치 못한 모든 예외 (버그, 인프라 오류 등 → error 로그)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("예상치 못한 오류 발생", e);
        Sentry.captureException(e);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "알 수 없는 오류가 발생했습니다.");
    }

    private ResponseEntity<ApiResponse<?>> createErrorResponse(HttpStatus status, String errorCode, String message) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(status, errorCode, message, MDC.get("requestId"), null));
    }
}