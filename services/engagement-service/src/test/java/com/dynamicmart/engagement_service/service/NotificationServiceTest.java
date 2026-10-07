package com.dynamicmart.engagement_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dynamicmart.engagement_service.entity.Notification;
import com.dynamicmart.engagement_service.mapper.NotificationMapper;
import com.dynamicmart.engagement_service.repository.NotificationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {
    private final NotificationRepository notifications = org.mockito.Mockito.mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(notifications, new NotificationMapper());

    @Test
    void markReadSetsReadAtForOwnedNotification() {
        UUID customerId = UUID.randomUUID();
        Notification notification = new Notification(customerId, "ORDER_COMPLETED", "Hoàn tất", "Đơn hàng đã hoàn tất.", Instant.now());
        when(notifications.findByIdAndCustomerId(notification.getId(), customerId)).thenReturn(Optional.of(notification));

        var response = service.markRead(customerId, notification.getId());

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }
}
