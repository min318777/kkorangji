package com.min.meow.notification.service;


import com.min.kafka.dto.NotificationDto;
import com.min.meow.notification.dto.NotificationResponse;
import com.min.meow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 모든 알림 조회
    public Page<NotificationResponse> getAllNotifications(Pageable pageable){
        return notificationRepository.findAll(pageable)
                .map(NotificationResponse::from);
    }
}
