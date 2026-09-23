package com.dynamicmart.order_service.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(String code, String message, List<FieldViolation> errors, Instant timestamp) {
    public record FieldViolation(String field, String message) { }
}
