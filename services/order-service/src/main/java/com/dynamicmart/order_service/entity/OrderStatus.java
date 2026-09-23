package com.dynamicmart.order_service.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PACKING,
    SHIPPING,
    HANDOVER_PENDING,
    DELIVERED,
    COMPLETED,
    CANCELLED
}
