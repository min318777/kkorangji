package com.min.meow.global.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    
    // 게시글 예외
    NOT_FOUND_POST(HttpStatus.NOT_FOUND, "존재하지 않은 게시글입니다."),
    NOT_FOUND_COMMENT(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    
    // 회원 예외
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    UNREGISTERED_USER(HttpStatus.UNAUTHORIZED, "회원가입이 필요합니다." ),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ALREADY_EXISTING_USER(HttpStatus.CONFLICT, "동일한 아이디가 존재합니다."),
    ALREADY_EXISTING_EMAIL(HttpStatus.CONFLICT, "동일한 이메일의 계정이 존재합니다." ),
    ALREADY_WITHDRAWN_USER(HttpStatus.BAD_REQUEST, "이미 탈퇴한 회원입니다."),
    WITHDRAWN_USER(HttpStatus.FORBIDDEN, "탈퇴한 회원은 이용할 수 없습니다."),

    // 역할/권한 예외
    NOT_FOUND_ROLE(HttpStatus.INTERNAL_SERVER_ERROR, "역할 정보를 찾을 수 없습니다."),


    // jwt 예외
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 access 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh 토큰입니다." ),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "토큰 타입이 일치하지 않습니다."),

    // 알림 예외
    NOT_FOUND_NOTIFICATION(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    FORBIDDEN_NOTIFICATION_ACCESS(HttpStatus.FORBIDDEN, "해당 알림에 접근할 권한이 없습니다."),

    // Validation 예외
    FORBIDDEN_NOT_AUTHOR(HttpStatus.FORBIDDEN, "작성자 본인만 수정, 삭제가 가능합니다." ),

    // 외부 서비스 예외
    S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."),

    ;


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message){
        this.status = status;
        this.message = message;
    }
}
