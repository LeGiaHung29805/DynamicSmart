package com.dynamicmart.order_service.entity;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
