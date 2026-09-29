package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.AddressGateway.AddressSnapshot;
import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.client.PaymentClient.QuoteValidationRequest;
import com.dynamicmart.order_service.client.PaymentClient.ShippingQuoteResponse;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingBreakdown;
import com.dynamicmart.order_service.service.OrderCreationContextReader.CreationContext;
import com.dynamicmart.order_service.service.OrderCreationContextReader.MoneySnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.QuoteSnapshot;
import com.dynamicmart.order_service.service.OrderCreationRevalidationService.ValidatedOrderInput;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ShippingQuoteConsumptionServiceTests {
    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID QUOTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void consumesQuoteWithCustomerAndAuthoritativeFingerprint() {
        PaymentClient payments = Mockito.mock(PaymentClient.class);
        when(payments.validateAndConsumeQuote(eq(QUOTE_ID), Mockito.any())).thenReturn(response(25_000));
        ShippingQuoteConsumptionService service = service(payments);

        service.consume(input());

        ArgumentCaptor<QuoteValidationRequest> request = ArgumentCaptor.forClass(QuoteValidationRequest.class);
        verify(payments).validateAndConsumeQuote(eq(QUOTE_ID), request.capture());
        assertEquals(CUSTOMER_ID, request.getValue().customerId());
        assertEquals("a".repeat(64), request.getValue().requestFingerprint());
    }

    @Test
    void rejectsPaymentResponseWhenPayableFeeChanged() {
        PaymentClient payments = Mockito.mock(PaymentClient.class);
        when(payments.validateAndConsumeQuote(eq(QUOTE_ID), Mockito.any())).thenReturn(response(24_000));

        OrderException exception = assertThrows(OrderException.class, () -> service(payments).consume(input()));

        assertEquals("SHIPPING_QUOTE_CONSUME_MISMATCH", exception.getCode());
    }

    @Test
    void rejectsExpiredEchoEvenWhenMoneyStillMatches() {
        PaymentClient payments = Mockito.mock(PaymentClient.class);
        ShippingQuoteResponse expired = new ShippingQuoteResponse(
                QUOTE_ID, 30_000, 5_000, 25_000, 53320, "GHN Standard", "2 ngày",
                NOW, "a".repeat(64));
        when(payments.validateAndConsumeQuote(eq(QUOTE_ID), Mockito.any())).thenReturn(expired);

        OrderException exception = assertThrows(OrderException.class, () -> service(payments).consume(input()));

        assertEquals("SHIPPING_QUOTE_CONSUME_MISMATCH", exception.getCode());
    }

    private ShippingQuoteConsumptionService service(PaymentClient payments) {
        return new ShippingQuoteConsumptionService(payments, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ShippingQuoteResponse response(long payable) {
        return new ShippingQuoteResponse(
                QUOTE_ID, 30_000, 5_000, payable, 53320, "GHN Standard", "2 ngày",
                NOW.plusSeconds(600), "a".repeat(64));
    }

    private ValidatedOrderInput input() {
        CreationContext context = new CreationContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CUSTOMER_ID,
                CheckoutSource.CART, UUID.randomUUID(), UUID.randomUUID(),
                PaymentTiming.PREPAID, PaymentMethod.VNPAY,
                new MoneySnapshot(100_000, 0, 100_000, 0, 0, 30_000, 5_000, 125_000),
                List.of(), List.of(),
                new QuoteSnapshot(
                        UUID.randomUUID(), QUOTE_ID, "GHN", "a".repeat(64),
                        30_000, 5_000, 25_000, 53320, "GHN Standard", null, "2 ngày",
                        100, 10, 10, 10, 1, 2, "Hà Nội", "Phường A", NOW.plusSeconds(600)));
        return new ValidatedOrderInput(
                context,
                new AddressSnapshot(context.addressId(), "Hiếu", "0900000000", "1 Đường A",
                        1, 2, "Hà Nội", "Phường A"),
                List.of(), List.of(),
                new PricingBreakdown(100_000, 0, 100_000, 0, 0, 30_000, 5_000, 125_000));
    }
}
