package com.dynamicmart.order_service.entity;

public enum SagaStatus {
    STARTED,
    VOUCHER_RESERVED,
    INVENTORY_RESERVED,
    ORDER_CREATED,
    PAYMENT_REQUESTED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
