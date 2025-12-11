package com.min.meow.notification.service;


import com.min.meow.notification.dto.NotificationResponse;
import com.min.meow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 왜 이렇게 구성했는가?
 * - 알림 저장은 도메인 모듈의 NotificationService가 담당
 * - 이 서비스는 API 레이어에서 필요한 조회 기능만 제공
 * - SSE 실시간 전송은 NotificationEventListener가 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public Page<NotificationResponse> getAllNotifications(Pageable pageable){
        return notificationRepository.findAll(pageable)
                .map(NotificationResponse::from);
    }
}
