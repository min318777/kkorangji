package com.min.notification.service;

import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.repository.NotificationRepository;
import com.min.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    /**
     * 모든 알림을 최신 순으로 조회
     * @param pageable 페이징 정보
     * @return 최신 순으로 정렬된 알림 목록
     */
    public Page<NotificationResponse> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(NotificationResponse::from);
    }

    /**
     * 단일 알림을 읽음 처리
     * - 권한 검증: 요청한 사용자가 알림의 수신자인지 확인
     * - JPA dirty checking을 통해 자동으로 DB 업데이트
     * @param notificationId 읽음 처리할 알림 ID
     * @param userLoginId 요청한 사용자의 로그인 ID
     * @return 읽음 처리된 알림 정보
     * @throws IllegalArgumentException 알림을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public NotificationResponse readSingleNotification(Long notificationId, String userLoginId) {
        log.info("단일 알림 읽음 처리 - NotificationId: {}, User: {}", notificationId, userLoginId);

        // 알림 조회 및 권한 검증 (본인의 알림인지 확인)
        Notification notification = notificationRepository
                .findByIdAndReceiverLoginId(notificationId, userLoginId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "알림을 찾을 수 없거나 접근 권한이 없습니다. ID: " + notificationId));

        // 이미 읽은 알림인 경우 로그만 남기고 그대로 반환
        if (notification.isRead()) {
            log.info("이미 읽은 알림입니다 - NotificationId: {}", notificationId);
            return NotificationResponse.from(notification);
        }

        // 읽음 처리 (JPA dirty checking으로 자동 업데이트)
        notification.markAsRead();
        log.info("알림 읽음 처리 완료 - NotificationId: {}", notificationId);

        return NotificationResponse.from(notification);
    }

    /**
     * 여러 개의 알림을 읽음 처리
     * - 권한 검증: 요청한 사용자의 알림만 조회 및 업데이트
     * - 존재하지 않는 ID나 권한 없는 알림은 무시됨
     * @param notificationIds 읽음 처리할 알림 ID 목록
     * @param userLoginId 요청한 사용자의 로그인 ID
     * @return 읽음 처리된 알림 개수
     */
    @Transactional
    public int readMultipleNotifications(List<Long> notificationIds, String userLoginId) {
        log.info("여러 개 알림 읽음 처리 - Count: {}, User: {}", notificationIds.size(), userLoginId);

        // 사용자의 알림만 조회 (권한 검증 포함)
        List<Notification> notifications = notificationRepository
                .findAllByIdInAndReceiverLoginId(notificationIds, userLoginId);

        if (notifications.isEmpty()) {
            log.warn("읽음 처리할 알림이 없습니다 - User: {}", userLoginId);
            return 0;
        }

        // 읽지 않은 알림만 읽음 처리
        int readCount = 0;
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notification.markAsRead();
                readCount++;
            }
        }

        log.info("알림 읽음 처리 완료 - 처리된 알림 수: {}/{}", readCount, notifications.size());
        return readCount;
    }

    /**
     * 특정 사용자의 모든 읽지 않은 알림을 읽음 처리
     * - 해당 사용자의 모든 미읽음 알림을 일괄 처리
     * @param userLoginId 요청한 사용자의 로그인 ID
     * @return 읽음 처리된 알림 개수
     */
    @Transactional
    public int readAllNotifications(String userLoginId) {
        log.info("전체 알림 읽음 처리 - User: {}", userLoginId);

        // 사용자의 읽지 않은 모든 알림 조회
        List<Notification> unreadNotifications = notificationRepository
                .findAllByReceiverLoginIdAndIsReadFalse(userLoginId);

        if (unreadNotifications.isEmpty()) {
            log.info("읽지 않은 알림이 없습니다 - User: {}", userLoginId);
            return 0;
        }

        // 모든 알림을 읽음 처리
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }

        int readCount = unreadNotifications.size();
        log.info("전체 알림 읽음 처리 완료 - 처리된 알림 수: {}", readCount);
        return readCount;
    }
}
