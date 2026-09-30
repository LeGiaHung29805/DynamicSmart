package com.dynamicmart.payment_service.api;

import java.util.List;

public record PaymentPageResponse(List<PaymentResponse> content, int page, int size, long totalElements, int totalPages) { }
