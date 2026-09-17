package com.dynamicmart.payment_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shipping_quotes")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PUBLIC)
public class ShippingQuote {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "province_id", nullable = false) private int provinceId;
    @Column(name = "ward_id", nullable = false) private int wardId;
    @Column(name = "items_fingerprint", nullable = false, length = 64) private String itemsFingerprint;
    @Column(name = "request_fingerprint", nullable = false, length = 64) private String requestFingerprint;
    @Column(name = "fee_vnd", nullable = false) private long feeVnd;
    @Column(name = "shipping_discount_vnd", nullable = false) private long shippingDiscountVnd;
    @Column(name = "service_id", nullable = false) private int serviceId;
    @Column(name = "service_name", nullable = false, length = 150) private String serviceName;
    @Column(name = "eta_text", length = 150) private String etaText;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
