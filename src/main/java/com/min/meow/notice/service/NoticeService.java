package com.min.meow.notice.service;


import com.min.meow.notice.NoticeDto;
import com.min.meow.notice.entity.Notice;
import com.min.meow.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 모든 알림 조회
    public Page<NoticeDto> getAllNotice(Pageable pageable){
        
        return noticeRepository.findAll(pageable).map(NoticeDto::convertToDto);
    }
    
    
    // kafka 컨슈머에서 호출
    public void saveNotice(NoticeDto noticeDto){

        Notice notice = Notice.builder()
                .commentId(noticeDto.getCommentId())
                .postId(noticeDto.getPostId())
                .message(noticeDto.getMessage())
                .receiverLoginId(noticeDto.getReceiverLoginId())
                .isRead(false)
                .message(noticeDto.getMessage())
                .build();

        noticeRepository.save(notice);
    }
}
