package com.dynamicmart.payment_service.dto.response;

import java.util.List;

public record PaymentPageResponse(List<PaymentResponse> content, int page, int size, long totalElements, int totalPages) { }
