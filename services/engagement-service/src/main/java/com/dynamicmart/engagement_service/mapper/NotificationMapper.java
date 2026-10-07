package com.dynamicmart.engagement_service.mapper;

import com.dynamicmart.engagement_service.dto.response.NotificationResponse;
import com.dynamicmart.engagement_service.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getContent(), notification.getReadAt() != null, notification.getReadAt(),
                notification.getCreatedAt());
    }
}
