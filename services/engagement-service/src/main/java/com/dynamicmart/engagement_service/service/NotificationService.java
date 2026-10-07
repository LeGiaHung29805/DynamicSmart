package com.dynamicmart.engagement_service.service;

import com.dynamicmart.engagement_service.dto.response.NotificationResponse;
import com.dynamicmart.engagement_service.dto.response.PageResponse;
import com.dynamicmart.engagement_service.entity.Notification;
import com.dynamicmart.engagement_service.exception.EngagementException;
import com.dynamicmart.engagement_service.mapper.NotificationMapper;
import com.dynamicmart.engagement_service.repository.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationMapper mapper;

    public NotificationService(NotificationRepository notifications, NotificationMapper mapper) {
        this.notifications = notifications;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID customerId, int page, int size) {
        var source = notifications.findAllByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size));
        return PageResponse.from(source.map(mapper::toResponse));
    }

    @Transactional
    public NotificationResponse markRead(UUID customerId, UUID notificationId) {
        Notification notification = notifications.findByIdAndCustomerId(notificationId, customerId)
                .orElseThrow(() -> new EngagementException(HttpStatus.NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo."));
        notification.markRead(Instant.now());
        return mapper.toResponse(notification);
    }

    public Notification create(UUID customerId, String type, String title, String content, Instant now) {
        return notifications.save(new Notification(customerId, type, title, content, now));
    }
}
