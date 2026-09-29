package com.dynamicmart.order_service.entity;

import java.io.Serializable;
import java.util.UUID;

public record IdempotencyRecordId(String operation, UUID idempotencyKey) implements Serializable {
}
