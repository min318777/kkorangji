package com.min.meow.notification.repository;


import com.min.meow.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 특정 사용자의 특정 알림 조회 (권한 검증용)
     * @param id 알림 ID
     * @param receiverLoginId 수신자 로그인 ID
     * @return 조회된 알림
     */
    Optional<Notification> findByIdAndReceiverLoginId(Long id, String receiverLoginId);

    /**
     * 특정 사용자의 여러 알림 조회 (ID 목록으로)
     * @param ids 알림 ID 목록
     * @param receiverLoginId 수신자 로그인 ID
     * @return 조회된 알림 목록
     */
    List<Notification> findAllByIdInAndReceiverLoginId(List<Long> ids, String receiverLoginId);

    /**
     * 특정 사용자의 읽지 않은 모든 알림 조회
     * @param receiverLoginId 수신자 로그인 ID
     * @return 읽지 않은 알림 목록
     */
    List<Notification> findAllByReceiverLoginIdAndIsReadFalse(String receiverLoginId);

}
