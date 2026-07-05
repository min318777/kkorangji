package com.min.meow.common;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;


@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON 직렬화에서 제외 (성공 시 errorCode:null 미노출)
@Schema(description = "공통 API 응답 래퍼")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    @Schema(description = "HTTP 상태 코드", example = "200")
    private int status;

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    // 실패 시 ErrorCode enum 이름 — 프론트가 문자열 비교 없이 정확히 분기 가능, 성공 시 null
    @Schema(description = "에러 코드 (성공 시 null)", example = "NOT_FOUND_POST")
    private String errorCode;

    @Schema(description = "응답 메시지", example = "조회 성공")
    private String message;

    @Schema(description = "응답 데이터 (실패 시 null)")
    private T data;

    /** 200 OK 성공 응답 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .success(true)
                .errorCode(null)
                .message(message)
                .data(data)
                .build();
    }

    /** 201 Created 성공 응답 */
    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.CREATED.value())
                .success(true)
                .errorCode(null)
                .message(message)
                .data(data)
                .build();
    }

    /** 실패 응답 — errorCode 포함 */
    public static <T> ApiResponse<T> fail(HttpStatus status, String errorCode, String message) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .data(null)
                .build();
    }

    /** 실패 응답 — errorCode + data 포함 (검증 에러 목록 등) */
    public static <T> ApiResponse<T> fail(HttpStatus status, String errorCode, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .data(data)
                .build();
    }
}
