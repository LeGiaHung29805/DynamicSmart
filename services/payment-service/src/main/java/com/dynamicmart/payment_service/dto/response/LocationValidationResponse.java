package com.dynamicmart.payment_service.dto.response;

public record LocationValidationResponse(boolean valid, int provinceId, int wardId) { }
