package com.dynamicmart.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "checkout_shipping_quotes")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckoutShippingQuote {
    @Id private UUID id;
    @Column(name = "checkout_session_id", nullable = false) private UUID checkoutSessionId;
    @Column(name = "quote_id", nullable = false) private UUID quoteId;
    @Column(nullable = false, length = 30) private String provider;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "input_fingerprint", nullable = false, length = 64, columnDefinition = "char(64)")
    private String inputFingerprint;
    @Column(name = "fee_vnd", nullable = false) private long feeVnd;
    @Column(name = "shipping_discount_vnd", nullable = false) private long shippingDiscountVnd;
    @Column(name = "payable_fee_vnd", nullable = false) private long payableFeeVnd;
    @Column(name = "service_id", nullable = false) private int serviceId;
    @Column(name = "service_name", length = 200) private String serviceName;
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
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "quoted_at", nullable = false) private Instant quotedAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private ShippingQuoteStatus status;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response_redacted", columnDefinition = "jsonb") private String rawResponseRedacted;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static CheckoutShippingQuote create(
            UUID id,
            UUID checkoutSessionId,
            UUID quoteId,
            String inputFingerprint,
            long feeVnd,
            long shippingDiscountVnd,
            long payableFeeVnd,
            int serviceId,
            String serviceName,
            String etaText,
            int totalWeightGrams,
            int packageLengthCm,
            int packageWidthCm,
            int packageHeightCm,
            int toProvinceId,
            int toWardId,
            String toProvinceName,
            String toWardName,
            Instant expiresAt,
            Instant now) {
        CheckoutShippingQuote quote = new CheckoutShippingQuote();
        quote.id = id;
        quote.checkoutSessionId = checkoutSessionId;
        quote.quoteId = quoteId;
        quote.provider = "GHN";
        quote.refresh(inputFingerprint, feeVnd, shippingDiscountVnd, payableFeeVnd, serviceId, serviceName,
                etaText, totalWeightGrams, packageLengthCm, packageWidthCm, packageHeightCm,
                toProvinceId, toWardId, toProvinceName, toWardName, expiresAt, now);
        quote.createdAt = now;
        return quote;
    }

    public void refresh(
            String inputFingerprint,
            long feeVnd,
            long shippingDiscountVnd,
            long payableFeeVnd,
            int serviceId,
            String serviceName,
            String etaText,
            int totalWeightGrams,
            int packageLengthCm,
            int packageWidthCm,
            int packageHeightCm,
            int toProvinceId,
            int toWardId,
            String toProvinceName,
            String toWardName,
            Instant expiresAt,
            Instant now) {
        this.inputFingerprint = inputFingerprint;
        this.feeVnd = feeVnd;
        this.shippingDiscountVnd = shippingDiscountVnd;
        this.payableFeeVnd = payableFeeVnd;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.etaText = etaText;
        this.totalWeightGrams = totalWeightGrams;
        this.packageLengthCm = packageLengthCm;
        this.packageWidthCm = packageWidthCm;
        this.packageHeightCm = packageHeightCm;
        this.toProvinceId = toProvinceId;
        this.toWardId = toWardId;
        this.toProvinceName = toProvinceName;
        this.toWardName = toWardName;
        this.expiresAt = expiresAt;
        this.quotedAt = now;
        this.consumedAt = null;
        this.status = ShippingQuoteStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void invalidate(Instant now) {
        if (status == ShippingQuoteStatus.ACTIVE) {
            status = ShippingQuoteStatus.INVALIDATED;
            updatedAt = now;
        }
    }

    public void consume(Instant now) {
        status = ShippingQuoteStatus.CONSUMED;
        consumedAt = now;
        updatedAt = now;
    }
}
