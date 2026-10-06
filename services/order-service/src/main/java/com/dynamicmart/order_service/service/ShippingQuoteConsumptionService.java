package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.client.PaymentClient.QuoteValidationRequest;
import com.dynamicmart.order_service.client.PaymentClient.ShippingQuoteResponse;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.service.OrderCreationContextReader.QuoteSnapshot;
import com.dynamicmart.order_service.service.OrderCreationRevalidationService.ValidatedOrderInput;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Consumes the provider quote outside the Order database transaction and validates the echoed contract. */
@Service
public class ShippingQuoteConsumptionService {
    private final PaymentClient payments;
    private final Clock clock;

    public ShippingQuoteConsumptionService(PaymentClient payments, Clock clock) {
        this.payments = payments;
        this.clock = clock;
    }

    public void consume(ValidatedOrderInput input) {
        QuoteSnapshot expected = input.context().quote();
        ShippingQuoteResponse consumed = payments.validateAndConsumeQuote(
                expected.providerQuoteId(),
                new QuoteValidationRequest(input.context().customerId(), expected.inputFingerprint()));
        Instant now = Instant.now(clock);
        if (consumed == null
                || !Objects.equals(consumed.quoteId(), expected.providerQuoteId())
                || !Objects.equals(consumed.requestFingerprint(), expected.inputFingerprint())
                || consumed.feeVnd() != expected.feeVnd()
                || consumed.shippingDiscountVnd() != expected.shippingDiscountVnd()
                || consumed.payableFeeVnd() != expected.payableFeeVnd()
                || consumed.serviceId() != expected.serviceId()
                || !Objects.equals(consumed.serviceName(), expected.serviceName())
                || consumed.expiresAt() == null
                || !consumed.expiresAt().isAfter(now)) {
            throw new OrderException(
                    HttpStatus.CONFLICT,
                    "SHIPPING_QUOTE_CONSUME_MISMATCH",
                    "Payment Service trả báo giá không còn khớp Checkout đã revalidate.");
        }
    }
}
