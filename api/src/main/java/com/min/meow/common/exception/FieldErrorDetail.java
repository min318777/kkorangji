package com.min.meow.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 필드 검증 에러 상세 정보를 담는 불변 DTO
 * record를 사용하여 equals, hashCode, toString, 생성자를 자동 생성 (Java 16+)
 */
@Schema(description = "필드 검증 에러 상세")
public record FieldErrorDetail(

        @Schema(description = "검증 실패 필드명", example = "email")
        String field,

        @Schema(description = "검증 실패 메시지", example = "올바른 이메일 형식이 아닙니다")
        String message
) {}
