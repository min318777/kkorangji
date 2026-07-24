package com.min.meow.common.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 게시글 예외
    NOT_FOUND_POST(HttpStatus.NOT_FOUND, "존재하지 않은 게시글입니다."),
    NOT_FOUND_COMMENT(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 댓글을 달 수 없습니다."),

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
    ALREADY_RESTRICTED_USER(HttpStatus.BAD_REQUEST, "이미 제한된 사용자입니다."),
    NOT_RESTRICTED_USER(HttpStatus.BAD_REQUEST, "제한된 상태의 사용자가 아닙니다."),
    CANNOT_MANAGE_ADMIN(HttpStatus.FORBIDDEN, "관리자 계정은 제한하거나 탈퇴시킬 수 없습니다."),
    CANNOT_MANAGE_SELF(HttpStatus.FORBIDDEN, "본인 계정은 관리할 수 없습니다."),


    // jwt 예외
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 access 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh 토큰입니다." ),
    INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "토큰 타입이 일치하지 않습니다."),

    // 알림 예외
    NOT_FOUND_NOTIFICATION(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    FORBIDDEN_NOTIFICATION_ACCESS(HttpStatus.FORBIDDEN, "해당 알림에 접근할 권한이 없습니다."),

    // 좋아요 예외
    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 누른 게시글입니다."),
    NOT_LIKED(HttpStatus.BAD_REQUEST, "좋아요를 누르지 않은 게시글입니다."),

    // Validation 예외
    FORBIDDEN_NOT_AUTHOR(HttpStatus.FORBIDDEN, "작성자 본인만 수정, 삭제가 가능합니다." ),
    INVALID_POST_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 게시글 타입입니다. BOAST 또는 LOST만 허용됩니다."),

    // 외부 서비스 예외
    S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."),
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),

    // 이미지 예외
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. 허용 형식: image/jpeg, image/png, image/gif, image/webp"),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기가 허용 범위를 초과했습니다."),

    // 검색 예외
    SEARCH_KEYWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "검색어는 2글자 이상이어야 합니다."),

    ;

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message){
        this.status = status;
        this.message = message;
    }
}
