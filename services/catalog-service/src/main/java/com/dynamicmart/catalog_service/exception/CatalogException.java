package com.dynamicmart.catalog_service.exception;

import org.springframework.http.HttpStatus;

public class CatalogException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public CatalogException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
