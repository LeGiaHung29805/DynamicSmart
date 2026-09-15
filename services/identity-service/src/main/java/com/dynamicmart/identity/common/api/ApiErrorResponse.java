package com.dynamicmart.identity.common.api;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(String code, String message, List<FieldError> errors, Instant timestamp) {
    public record FieldError(String field, String message) { }
}
