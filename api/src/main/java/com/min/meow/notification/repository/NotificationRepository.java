package com.min.meow.notification.repository;


import com.min.meow.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 사용자의 알림만 최신 순으로 조회
    Page<Notification> findAllByReceiverUserIdOrderByCreatedAtDesc(Long receiverUserId, Pageable pageable);

    /**
     * 특정 사용자의 여러 알림 조회 (ID 목록으로)
     * @param ids 알림 ID 목록
     * @param receiverUserId 수신자 사용자 ID (PK)
     * @return 조회된 알림 목록
     */
    List<Notification> findAllByIdInAndReceiverUserId(List<Long> ids, Long receiverUserId);

    /**
     * 특정 사용자의 읽지 않은 모든 알림 조회
     * @param receiverUserId 수신자 사용자 ID (PK)
     * @return 읽지 않은 알림 목록
     */
    List<Notification> findAllByReceiverUserIdAndIsReadFalse(Long receiverUserId);

}
