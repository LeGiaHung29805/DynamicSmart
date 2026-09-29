package com.dynamicmart.order_service.config;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentCustomer {
    public UUID idFrom(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new OrderException(HttpStatus.UNAUTHORIZED, "INVALID_JWT_SUBJECT",
                    "JWT subject phải là UUID của khách hàng.");
        }
    }
}
