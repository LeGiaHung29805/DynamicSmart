package com.dynamicmart.notification_service.controller;

import com.dynamicmart.notification_service.exception.NotificationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

final class CurrentUser {
    private CurrentUser() {
    }

    static UUID id(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new NotificationException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Người dùng chưa xác thực.");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new NotificationException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_SUBJECT", "Định danh người dùng không hợp lệ.");
        }
    }
}
