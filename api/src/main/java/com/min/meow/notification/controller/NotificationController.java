package com.min.meow.notification.controller;


import com.min.meow.global.PageResponse;
import com.min.meow.global.exception.ErrorResponse;
import com.min.meow.notification.NotificationDto;
import com.min.meow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/notice")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getAllNotice(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam (defaultValue = "5") int size){

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDto> notices = notificationService.getAllNotice(pageable);
        PageResponse<NotificationDto> pageResponse = PageResponse.from(notices);

        return ResponseEntity.ok(new ErrorResponse<>(true, "댓글 알림 조회 성공", pageResponse));
    }
}
