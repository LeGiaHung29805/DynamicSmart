package com.dynamicmart.payment_service.api;

public record LocationValidationResponse(boolean valid, int provinceId, int wardId) { }
