package com.dynamicmart.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_shipping_snapshots")
@Getter @Setter @NoArgsConstructor
public class OrderShippingSnapshot {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "quote_id", nullable = false) private UUID quoteId;
    @Column(name = "input_fingerprint", nullable = false, length = 64) private String inputFingerprint;
    @Column(name = "service_id", nullable = false) private int serviceId;
    @Column(name = "service_name", length = 200) private String serviceName;
    @Column(name = "fee_vnd", nullable = false) private long feeVnd;
    @Column private Instant eta;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
