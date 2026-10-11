package com.dynamicmart.notification_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.dynamicmart.notification_service.dto.response.NotificationResponse;
import com.dynamicmart.notification_service.dto.response.PageResponse;
import com.dynamicmart.notification_service.entity.Notification;
import com.dynamicmart.notification_service.exception.NotificationException;
import com.dynamicmart.notification_service.mapper.NotificationMapper;
import com.dynamicmart.notification_service.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class NotificationServiceTest {
    private NotificationRepository notifications;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notifications = org.mockito.Mockito.mock(NotificationRepository.class);
        service = new NotificationService(notifications, new NotificationMapper());
    }

    @Test
    void markReadSetsReadAtForOwnedNotification() {
        UUID customerId = UUID.randomUUID();
        Notification notification = new Notification(customerId, "ORDER_COMPLETED", "Hoàn tất", "Đơn hàng đã hoàn tất.", Instant.now());
        when(notifications.findByIdAndCustomerId(notification.getId(), customerId)).thenReturn(Optional.of(notification));

        var response = service.markRead(customerId, notification.getId());

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
    }

    @Test
    void markReadNotFoundThrowsNotificationException() {
        UUID customerId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(notifications.findByIdAndCustomerId(notificationId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(customerId, notificationId))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("Không tìm thấy thông báo");
    }

    @Test
    void listReturnsPaginatedNotifications() {
        UUID customerId = UUID.randomUUID();
        Notification n1 = new Notification(customerId, "ORDER_COMPLETED", "Hoàn tất", "Đơn hàng #1", Instant.now());
        when(notifications.findAllByCustomerIdOrderByCreatedAtDesc(eq(customerId), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(n1), PageRequest.of(0, 20), 1));

        PageResponse<NotificationResponse> result = service.list(customerId, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).title()).isEqualTo("Hoàn tất");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void listWithNullCustomerIdThrowsUnauthorized() {
        assertThatThrownBy(() -> service.list(null, 0, 20))
                .isInstanceOf(NotificationException.class);
    }
}
