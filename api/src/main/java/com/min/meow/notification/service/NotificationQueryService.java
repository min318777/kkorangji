package com.min.meow.notification.service;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.repository.NotificationRepository;
import com.min.meow.notification.dto.response.NotificationResponse;
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
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    /**
     * 모든 알림을 최신 순으로 조회
     * @param pageable 페이징 정보
     * @return 최신 순으로 정렬된 알림 목록
     */
    public Page<NotificationResponse> getAllNotifications(Long userId, Pageable pageable) {
        // 본인 알림만 조회
        return notificationRepository.findAllByReceiverUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    /**
     * 단일 알림을 읽음 처리
     * - 1단계: 알림 존재 여부 확인 (없으면 404)
     * - 2단계: 본인 알림인지 권한 검증 (아니면 403)
     * @param notificationId 읽음 처리할 알림 ID
     * @param userId 요청한 사용자의 ID (PK)
     * @return 읽음 처리된 알림 정보
     */
    @Transactional
    public NotificationResponse readSingleNotification(Long notificationId, Long userId) {
        log.debug("단일 알림 읽음 처리 - NotificationId: {}, UserId: {}", notificationId, userId);

        // 1단계: 알림 존재 여부 확인
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_NOTIFICATION));

        // 2단계: 본인의 알림인지 권한 검증
        if (!notification.getReceiverUserId().equals(userId)) {
            log.warn("알림 접근 권한 없음 - UserId: {}", userId);
            throw new CustomException(ErrorCode.FORBIDDEN_NOTIFICATION_ACCESS);
        }

        // 이미 읽은 알림인 경우 로그만 남기고 그대로 반환
        if (notification.isRead()) {
            log.debug("이미 읽은 알림입니다 - NotificationId: {}", notificationId);
            return NotificationResponse.from(notification);
        }

        // 읽음 처리 (JPA dirty checking으로 자동 업데이트)
        notification.markAsRead();
        log.debug("알림 읽음 처리 완료 - NotificationId: {}", notificationId);

        return NotificationResponse.from(notification);
    }

    /**
     * 여러 개의 알림을 읽음 처리
     * @param notificationIds 읽음 처리할 알림 ID 목록
     * @param userId 요청한 사용자의 ID (PK)
     * @return 읽음 처리된 알림 개수
     */
    @Transactional
    public int readMultipleNotifications(List<Long> notificationIds, Long userId) {
        log.debug("여러 개 알림 읽음 처리 - Count: {}, UserId: {}", notificationIds.size(), userId);

        // 사용자의 알림만 조회 (권한 검증 포함)
        List<Notification> notifications = notificationRepository
                .findAllByIdInAndReceiverUserId(notificationIds, userId);

        if (notifications.isEmpty()) {
            log.warn("읽음 처리할 알림이 없습니다 - UserId: {}", userId);
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

        log.debug("알림 읽음 처리 완료 - 처리된 알림 수: {}/{}", readCount, notifications.size());
        return readCount;
    }

    /**
     * SSE 재연결 시 lastEventId 이후 누락된 알림 조회
     */
    public List<NotificationResponse> getMissedNotifications(Long userId, Long lastEventId) {
        return notificationRepository
                .findByReceiverUserIdAndIdGreaterThanOrderByIdAsc(userId, lastEventId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /**
     * 특정 사용자의 모든 읽지 않은 알림을 읽음 처리
     * @param userId 요청한 사용자의 ID (PK)
     * @return 읽음 처리된 알림 개수
     */
    @Transactional
    public int readAllNotifications(Long userId) {
        log.debug("전체 알림 읽음 처리 - UserId: {}", userId);

        // 사용자의 읽지 않은 모든 알림 조회
        List<Notification> unreadNotifications = notificationRepository
                .findAllByReceiverUserIdAndIsReadFalse(userId);

        if (unreadNotifications.isEmpty()) {
            log.debug("읽지 않은 알림이 없습니다 - UserId: {}", userId);
            return 0;
        }

        // 모든 알림을 읽음 처리
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }

        int readCount = unreadNotifications.size();
        log.debug("전체 알림 읽음 처리 완료 - 처리된 알림 수: {}", readCount);
        return readCount;
    }
}
