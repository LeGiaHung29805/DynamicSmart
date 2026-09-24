package com.dynamicmart.order_service.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "order_shipping_snapshots")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderShippingSnapshot {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "quote_id", nullable = false) private UUID quoteId;
    @Column(name = "source_checkout_quote_id") private UUID sourceCheckoutQuoteId;
    @Column(nullable = false, length = 30) private String provider;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "input_fingerprint", nullable = false, length = 64, columnDefinition = "char(64)")
    private String inputFingerprint;
    @Column(name = "service_id", nullable = false) private int serviceId;
    @Column(name = "service_name", length = 200) private String serviceName;
    @Column(name = "fee_vnd", nullable = false) private long feeVnd;
    @Column(name = "shipping_discount_vnd", nullable = false) private long shippingDiscountVnd;
    @Column(name = "payable_fee_vnd", nullable = false) private long payableFeeVnd;
    @Column(name = "eta") private Instant eta;
    @Column(name = "eta_text", length = 200) private String etaText;
    @Column(name = "total_weight_grams") private Integer totalWeightGrams;
    @Column(name = "package_length_cm") private Integer packageLengthCm;
    @Column(name = "package_width_cm") private Integer packageWidthCm;
    @Column(name = "package_height_cm") private Integer packageHeightCm;
    @Column(name = "to_province_id") private Integer toProvinceId;
    @Column(name = "to_ward_id") private Integer toWardId;
    @Column(name = "to_province_name", length = 150) private String toProvinceName;
    @Column(name = "to_ward_name", length = 150) private String toWardName;
    @Column(name = "quoted_at") private Instant quotedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response_redacted", columnDefinition = "jsonb") private String rawResponseRedacted;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static OrderShippingSnapshot create(
            UUID id,
            UUID orderId,
            UUID quoteId,
            UUID sourceCheckoutQuoteId,
            String provider,
            String inputFingerprint,
            int serviceId,
            String serviceName,
            long feeVnd,
            long shippingDiscountVnd,
            long payableFeeVnd,
            Instant eta,
            String etaText,
            Integer totalWeightGrams,
            Integer packageLengthCm,
            Integer packageWidthCm,
            Integer packageHeightCm,
            Integer toProvinceId,
            Integer toWardId,
            String toProvinceName,
            String toWardName,
            Instant quotedAt,
            String rawResponseRedacted,
            Instant now) {
        OrderShippingSnapshot shipping = new OrderShippingSnapshot();
        shipping.id = id;
        shipping.orderId = orderId;
        shipping.quoteId = quoteId;
        shipping.sourceCheckoutQuoteId = sourceCheckoutQuoteId;
        shipping.provider = provider;
        shipping.inputFingerprint = inputFingerprint;
        shipping.serviceId = serviceId;
        shipping.serviceName = serviceName;
        shipping.feeVnd = feeVnd;
        shipping.shippingDiscountVnd = shippingDiscountVnd;
        shipping.payableFeeVnd = payableFeeVnd;
        shipping.eta = eta;
        shipping.etaText = etaText;
        shipping.totalWeightGrams = totalWeightGrams;
        shipping.packageLengthCm = packageLengthCm;
        shipping.packageWidthCm = packageWidthCm;
        shipping.packageHeightCm = packageHeightCm;
        shipping.toProvinceId = toProvinceId;
        shipping.toWardId = toWardId;
        shipping.toProvinceName = toProvinceName;
        shipping.toWardName = toWardName;
        shipping.quotedAt = quotedAt;
        shipping.rawResponseRedacted = rawResponseRedacted;
        shipping.createdAt = now;
        return shipping;
    }
}
