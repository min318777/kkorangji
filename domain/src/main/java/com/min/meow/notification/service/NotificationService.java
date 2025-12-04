package com.min.meow.notification.service;

import com.min.meow.notification.CreateNotificationCommand;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void saveNotification(CreateNotificationCommand command){

        Notification notification = Notification.builder()
                .sourceId(command.sourceId())
                .postId(command.postId())
                .receiverLoginId(command.receiverLoginId())
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }
}
