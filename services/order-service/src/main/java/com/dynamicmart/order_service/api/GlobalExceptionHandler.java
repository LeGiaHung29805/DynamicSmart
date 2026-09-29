package com.dynamicmart.order_service.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderException.class)
    ResponseEntity<Map<String, Object>> order(OrderException exception) {
        return ResponseEntity.status(exception.status()).body(Map.of("timestamp", Instant.now().toString(),
                "code", exception.code(), "message", exception.getMessage()));
    }
}
