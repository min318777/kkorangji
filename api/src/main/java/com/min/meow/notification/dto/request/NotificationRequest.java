package com.min.meow.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 개의 알림을 읽음 처리하기 위한 요청 DTO
 */
@Schema(description = "다건 알림 읽음 처리 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @Schema(description = "읽음 처리할 알림 ID 목록", example = "[1, 2, 3]")
    @NotEmpty(message = "읽음 처리할 알림 id가 필요합니다.")
    private List<Long> notificationIds;
}
