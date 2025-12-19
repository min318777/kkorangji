package com.min.notification.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 개의 알림을 읽음 처리하기 위한 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    @NotEmpty(message = "읽음 처리할 알림 id가 필요합니다.")
    private List<Long> notificationIds;
}
