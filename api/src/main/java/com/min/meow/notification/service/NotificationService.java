package com.min.meow.notification.service;


import com.min.meow.notification.domain.NotificationDto;
import com.min.meow.notification.entity.Notification;
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
    public Page<NotificationDto> getAllNotice(Pageable pageable){

        return notificationRepository.findAll(pageable).map(NotificationDto::toDto);

    }
    
    
    // kafka 컨슈머에서 호출
    public void saveNotification(NotificationDto notificationDto){

        Notification notification = Notification.builder()
                .commentId(notificationDto.getCommentId())
                .postId(notificationDto.getPostId())
                .message(notificationDto.getMessage())
                .receiverLoginId(notificationDto.getReceiverLoginId())
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }
}
