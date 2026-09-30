package com.dynamicmart.cart_service.common.exception;

import com.dynamicmart.cart_service.common.api.ApiErrorResponse;
import com.dynamicmart.cart_service.exception.CartException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CartException.class)
    ResponseEntity<ApiErrorResponse> cart(CartException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiErrorResponse(exception.getCode(), exception.getMessage(), List.of(), Instant.now()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(value -> new ApiErrorResponse.FieldError(value.getField(), value.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ.", errors, Instant.now()));
    }
}
