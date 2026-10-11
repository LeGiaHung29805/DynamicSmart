package com.dynamicmart.notification_service.service;

import com.dynamicmart.notification_service.dto.response.NotificationResponse;
import com.dynamicmart.notification_service.dto.response.PageResponse;
import com.dynamicmart.notification_service.entity.Notification;
import com.dynamicmart.notification_service.exception.NotificationException;
import com.dynamicmart.notification_service.mapper.NotificationMapper;
import com.dynamicmart.notification_service.repository.NotificationRepository;
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
        if (customerId == null) {
            throw new NotificationException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Yêu cầu đăng nhập.");
        }
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        var source = notifications.findAllByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(safePage, safeSize));
        return PageResponse.from(source.map(mapper::toResponse));
    }

    @Transactional
    public NotificationResponse markRead(UUID customerId, UUID notificationId) {
        if (customerId == null) {
            throw new NotificationException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Yêu cầu đăng nhập.");
        }
        if (notificationId == null) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "NOTIFICATION_ID_REQUIRED", "Mã thông báo là bắt buộc.");
        }
        Notification notification = notifications.findByIdAndCustomerId(notificationId, customerId)
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo."));
        notification.markRead(Instant.now());
        return mapper.toResponse(notification);
    }

    public Notification create(UUID customerId, String type, String title, String content, Instant now) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId không được để trống.");
        }
        return notifications.save(new Notification(customerId, type, title, content, now != null ? now : Instant.now()));
    }
}
