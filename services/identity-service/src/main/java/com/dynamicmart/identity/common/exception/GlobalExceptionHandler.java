package com.dynamicmart.identity.common.exception;

import com.dynamicmart.identity.auth.application.AuthException;
import com.dynamicmart.identity.common.api.ApiErrorResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthException.class)
    ResponseEntity<ApiErrorResponse> handleAuth(AuthException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiErrorResponse(
                exception.getCode(), exception.getMessage(), List.of(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_FAILED", "Dữ liệu gửi lên không hợp lệ.", errors, Instant.now()));
    }
}
