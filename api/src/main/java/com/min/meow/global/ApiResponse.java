package com.min.meow.global;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 통합 API 응답 클래스
 *
 * 성공/실패 모두 이 클래스 하나로 처리하여 프론트엔드 파싱 일관성 보장
 *
 * 성공 응답 예시:
 * { "status": 200, "success": true, "message": "조회 성공", "data": {...} }
 *
 * 실패 응답 예시:
 * { "status": 400, "success": false, "message": "제목을 입력해 주세요.", "data": null }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int status;       // HTTP 상태 코드 숫자값 (200, 201, 400, 404, 500)
    private boolean success;  // 성공/실패 여부
    private String message;   // 응답 메시지
    private T data;           // 응답 데이터 (실패 시 null)

    // ==================== 성공 응답 팩토리 메서드 ====================

    /**
     * 200 OK 성공 응답 (조회, 수정 등)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 201 Created 성공 응답 (리소스 생성)
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.CREATED.value())
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // ==================== 실패 응답 팩토리 메서드 ====================

    /**
     * 실패 응답 (상태 코드 직접 지정)
     */
    public static <T> ApiResponse<T> fail(HttpStatus status, String message) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}