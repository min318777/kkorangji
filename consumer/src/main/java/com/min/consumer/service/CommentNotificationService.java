package com.min.consumer.service;

import com.min.kafka.dto.NotificationDto;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.repository.NotificationRepository;
import com.min.meow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentNotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void commentNotice(NotificationDto notificationDto){

        Notification notification = Notification.toEntity(notificationDto);
        notificationRepository.save(notification);
    }
}
