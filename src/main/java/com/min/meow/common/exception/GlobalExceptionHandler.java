package com.min.meow.common.exception;

import com.min.meow.common.ApiResponse;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
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
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getStatus(), errorCode.name(), errorCode.getMessage(), fieldErrors));
    }

    // 클라이언트 실수 — Path variable 타입 불일치 (예: /posts/abc, id가 Long이어야 할 때)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 불일치: {} = {}", e.getName(), e.getValue());
        return createErrorResponse(ErrorCode.INVALID_PARAMETER_TYPE,
                e.getName() + " 파라미터 형식이 올바르지 않습니다.");
    }

    // 클라이언트 실수 — 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락: {}", e.getParameterName());
        return createErrorResponse(ErrorCode.MISSING_PARAMETER,
                "필수 파라미터가 없습니다: " + e.getParameterName());
    }

    // 클라이언트 실수 — Request Body JSON 형식 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Request Body 파싱 실패: {}", e.getMessage());
        return createErrorResponse(ErrorCode.INVALID_REQUEST_BODY, ErrorCode.INVALID_REQUEST_BODY.getMessage());
    }

    // 클라이언트 실수 — IllegalArgumentException (잘못된 파일 형식 등 입력값 오류)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 입력값: {}", e.getMessage());
        return createErrorResponse(ErrorCode.INVALID_ARGUMENT, e.getMessage());
    }

    // 클라이언트 실수 — 존재하지 않는 URL 경로 요청
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("존재하지 않는 경로: {}", e.getResourcePath());
        return createErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    // 클라이언트 실수 — 잘못된 HTTP 메서드 (예: GET으로 POST 엔드포인트 호출)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 HTTP 메서드: {}", e.getMethod());
        return createErrorResponse(ErrorCode.METHOD_NOT_ALLOWED,
                "지원하지 않는 HTTP 메서드입니다: " + e.getMethod());
    }

    // 클라이언트 실수 — 잘못된 Content-Type (예: application/json 필요한데 누락)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 Content-Type: {}", e.getContentType());
        return createErrorResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage());
    }

    // Spring Security 예외 — 권한/인증 관련
    // 클라이언트 실수 — @PreAuthorize 권한 거부
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.warn("권한 없는 접근 시도: {}", e.getMessage());
        return createErrorResponse(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
    }

    // DB 예외 — 데이터 정합성 위반
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DB 제약 조건 위반: {}", e.getMessage());
        return createErrorResponse(ErrorCode.DATA_INTEGRITY_VIOLATION, ErrorCode.DATA_INTEGRITY_VIOLATION.getMessage());
    }

    // 특수 케이스 — 정상 동작이지만 예외로 처리되는 경우
    // 정상 동작 — SSE 연결 타임아웃 (클라이언트가 재연결하면 됨, 에러 아님)
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.debug("SSE 연결 타임아웃 - 클라이언트 재연결 필요");
        return ResponseEntity.noContent().build();
    }

    // 서버 실수 — 예상치 못한 모든 예외 (버그, 인프라 오류 등 → error 로그)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("예상치 못한 오류 발생", e);
        Sentry.captureException(e);
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    private ResponseEntity<ApiResponse<?>> createErrorResponse(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getStatus(), errorCode.name(), message, MDC.get("requestId"), null));
    }
}