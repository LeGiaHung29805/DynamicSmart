package com.dynamicmart.order_service.exception;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(OrderException.class)
    ResponseEntity<ApiErrorResponse> handleOrder(OrderException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiErrorResponse(
                exception.getCode(), exception.getMessage(), List.of(), Instant.now(clock)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldViolation> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ.", errors, Instant.now(clock)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String field = exception.getName() == null ? "parameter" : exception.getName();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "INVALID_REQUEST_PARAMETER",
                "Tham số truy vấn hoặc đường dẫn không hợp lệ.",
                List.of(new ApiErrorResponse.FieldViolation(field, "Giá trị không đúng định dạng.")),
                Instant.now(clock)));
    }

    private ApiErrorResponse.FieldViolation toViolation(FieldError error) {
        return new ApiErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage());
    }
}
