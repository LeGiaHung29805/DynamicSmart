package com.dynamicmart.reporting_service.exception;

import org.springframework.http.HttpStatus;

public class ReportingException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ReportingException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
