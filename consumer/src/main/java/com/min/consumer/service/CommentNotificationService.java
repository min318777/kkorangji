package com.min.consumer.service;

import com.min.kafka.dto.NotificationDto;
import com.min.meow.notice.entity.Notice;
import com.min.meow.notice.repository.NoticeRepository;
import com.min.meow.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentNotificationService {

    private final NoticeService noticeService;
    private final NoticeRepository noticeRepository;

    @Transactional
    public void commentNotice(NotificationDto notificationDto){

        Notice notice = Notice.toEntity(notificationDto);


        noticeRepository.save(notice);
    }
}
