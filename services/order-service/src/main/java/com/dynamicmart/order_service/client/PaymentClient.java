package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentClient {
    ShippingQuoteResponse createShippingQuote(ShippingQuoteRequest request);

    ShippingQuoteResponse validateAndConsumeQuote(UUID quoteId, QuoteValidationRequest request);

    PaymentResponse createPayment(OrderPaymentContextRequest request);

    PaymentResponse createVnPayAttempt(UUID paymentId);

    record ShippingItemRequest(
            UUID variantId,
            int quantity,
            int weightGrams,
            int lengthCm,
            int widthCm,
            int heightCm) {
    }

    record ShippingQuoteRequest(
            UUID customerId,
            int provinceId,
            int wardId,
            List<ShippingItemRequest> items,
            long shippingDiscountVnd,
            String serviceCode) {
    }

    record ShippingQuoteResponse(
            UUID quoteId,
            long feeVnd,
            long shippingDiscountVnd,
            long payableFeeVnd,
            int serviceId,
            String serviceName,
            String eta,
            Instant expiresAt,
            String requestFingerprint) {
    }

    record QuoteValidationRequest(UUID customerId, String requestFingerprint) {
    }

    record OrderPaymentContextRequest(
            UUID orderId,
            UUID customerId,
            long amountVnd,
            PaymentTiming timing,
            PaymentMethod method,
            UUID correlationId) {
    }

    record PaymentResponse(
            UUID id,
            UUID orderId,
            long amountVnd,
            String timing,
            String method,
            String status,
            String redirectUrl,
            Instant expiresAt,
            Instant paidAt) {
    }
}
