package com.dynamicmart.cart_service.service;

import com.dynamicmart.cart_service.exception.CartException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class InternalApiGuard {
    private final byte[] expected;
    public InternalApiGuard(@Value("${app.security.internal-api-key}") String key) {
        if (key == null || key.length() < 24) throw new IllegalStateException("INTERNAL_API_KEY phải có ít nhất 24 ký tự.");
        this.expected = key.getBytes(StandardCharsets.UTF_8);
    }
    public void verify(String supplied) {
        byte[] actual = supplied == null ? new byte[0] : supplied.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual))
            throw new CartException(HttpStatus.UNAUTHORIZED, "INTERNAL_API_KEY_INVALID", "Thông tin xác thực nội bộ không hợp lệ.");
    }
}
