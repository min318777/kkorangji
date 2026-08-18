package com.min.meow.notification.service;

import com.min.meow.common.exception.CustomException;
import com.min.meow.common.exception.ErrorCode;
import com.min.meow.common.NotificationType;
import com.min.meow.notification.entity.Notification;
import com.min.meow.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationQueryService 유닛 테스트")
class NotificationQueryServiceTest {

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Mock
    private NotificationRepository notificationRepository;

    private Notification createNotification(Long id, Long receiverUserId, boolean isRead) {
        return Notification.builder()
                .id(id)
                .sourceId(1L)
                .type(NotificationType.COMMENT)
                .message("댓글이 달렸습니다")
                .receiverUserId(receiverUserId)
                .isRead(isRead)
                .build();
    }

    @Nested
    @DisplayName("readSingleNotification() — 단건 읽음 처리")
    class ReadSingle {

        @Test
        @DisplayName("성공: 본인 알림이면 읽음 처리한다")
        void test_성공_읽음_처리() {
            // given
            Notification notification = createNotification(1L, 10L, false);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            // when
            notificationQueryService.readSingleNotification(1L, 10L);

            // then
            assertThat(notification.isRead()).isTrue();
        }

        @Test
        @DisplayName("성공: 이미 읽은 알림이면 예외 없이 그대로 반환한다")
        void test_성공_이미_읽은_알림() {
            // given
            Notification notification = createNotification(1L, 10L, true);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            // when & then — 예외 없이 정상 처리
            var response = notificationQueryService.readSingleNotification(1L, 10L);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 알림이면 NOT_FOUND_NOTIFICATION 예외를 던진다")
        void test_실패_존재하지_않는_알림() {
            // given
            given(notificationRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationQueryService.readSingleNotification(1L, 10L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND_NOTIFICATION);
        }

        @Test
        @DisplayName("실패: 타인의 알림이면 FORBIDDEN_NOTIFICATION_ACCESS 예외를 던진다")
        void test_실패_타인_알림_접근_차단() {
            // given
            Notification notification = createNotification(1L, 10L, false);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            // when & then — receiverUserId=10인데 userId=99로 요청
            assertThatThrownBy(() -> notificationQueryService.readSingleNotification(1L, 99L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN_NOTIFICATION_ACCESS);

            assertThat(notification.isRead()).isFalse();
        }
    }

    @Nested
    @DisplayName("readMultipleNotifications() — 다건 읽음 처리")
    class ReadMultiple {

        @Test
        @DisplayName("성공: 읽지 않은 알림만 카운트하여 반환한다")
        void test_성공_읽지_않은_것만_카운트() {
            // given
            Long userId = 10L;
            Notification unread1 = createNotification(1L, userId, false);
            Notification alreadyRead = createNotification(2L, userId, true);
            Notification unread2 = createNotification(3L, userId, false);

            given(notificationRepository.findAllByIdInAndReceiverUserId(List.of(1L, 2L, 3L), userId))
                    .willReturn(List.of(unread1, alreadyRead, unread2));

            // when
            int result = notificationQueryService.readMultipleNotifications(List.of(1L, 2L, 3L), userId);

            // then
            assertThat(result).isEqualTo(2);
            assertThat(unread1.isRead()).isTrue();
            assertThat(unread2.isRead()).isTrue();
        }

        @Test
        @DisplayName("성공: 대상 알림이 하나도 없으면(본인 것 아니거나 존재하지 않음) 0을 반환한다")
        void test_성공_대상_없음() {
            // given
            given(notificationRepository.findAllByIdInAndReceiverUserId(List.of(1L), 10L))
                    .willReturn(List.of());

            // when
            int result = notificationQueryService.readMultipleNotifications(List.of(1L), 10L);

            // then
            assertThat(result).isZero();
        }
    }

    @Nested
    @DisplayName("readAllNotifications() — 전체 읽음 처리")
    class ReadAll {

        @Test
        @DisplayName("성공: 읽지 않은 알림 전체를 읽음 처리하고 개수를 반환한다")
        void test_성공_전체_읽음_처리() {
            // given
            Long userId = 10L;
            Notification unread1 = createNotification(1L, userId, false);
            Notification unread2 = createNotification(2L, userId, false);
            given(notificationRepository.findAllByReceiverUserIdAndIsReadFalse(userId))
                    .willReturn(List.of(unread1, unread2));

            // when
            int result = notificationQueryService.readAllNotifications(userId);

            // then
            assertThat(result).isEqualTo(2);
            assertThat(unread1.isRead()).isTrue();
            assertThat(unread2.isRead()).isTrue();
        }

        @Test
        @DisplayName("성공: 읽지 않은 알림이 없으면 0을 반환한다")
        void test_성공_읽을_알림_없음() {
            // given
            given(notificationRepository.findAllByReceiverUserIdAndIsReadFalse(10L)).willReturn(List.of());

            // when
            int result = notificationQueryService.readAllNotifications(10L);

            // then
            assertThat(result).isZero();
        }
    }
}
