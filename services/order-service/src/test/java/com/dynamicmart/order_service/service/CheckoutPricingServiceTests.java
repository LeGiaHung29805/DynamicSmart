package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.service.CheckoutPricingService.LinePricing;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckoutPricingServiceTests {
    private static final UUID VARIANT_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VARIANT_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final CheckoutPricingService pricing = new CheckoutPricingService();

    @Test
    void calculatesEveryMoneyComponentFromTrustedLineInputs() {
        var result = pricing.calculate(new PricingRequest(List.of(
                new LinePricing(VARIANT_1, 100_000, 10_000, 2, 20_000, 10_000),
                new LinePricing(VARIANT_2, 50_000, 0, 1, 0, 5_000)), 30_000, 5_000));

        assertEquals(250_000, result.itemsListSubtotalVnd());
        assertEquals(20_000, result.directSaleDiscountVnd());
        assertEquals(230_000, result.itemsSubtotalVnd());
        assertEquals(20_000, result.productDiscountVnd());
        assertEquals(15_000, result.orderDiscountVnd());
        assertEquals(220_000, result.finalTotalVnd());
    }

    @Test
    void permitsAZeroValueCheckoutWithoutNegativeMoney() {
        var result = pricing.calculate(new PricingRequest(List.of(
                new LinePricing(VARIANT_1, 50_000, 0, 1, 20_000, 30_000)), 15_000, 15_000));

        assertEquals(0, result.finalTotalVnd());
    }

    @Test
    void rejectsShippingDiscountGreaterThanFee() {
        assertInvalid(() -> pricing.calculate(new PricingRequest(
                List.of(new LinePricing(VARIANT_1, 50_000, 0, 1, 0, 0)), 10_000, 10_001)));
    }

    @Test
    void rejectsDirectSaleGreaterThanListPrice() {
        assertInvalid(() -> pricing.calculate(new PricingRequest(
                List.of(new LinePricing(VARIANT_1, 50_000, 50_001, 1, 0, 0)), 0, 0)));
    }

    @Test
    void rejectsAllocatedDiscountGreaterThanLineSubtotal() {
        assertInvalid(() -> pricing.calculate(new PricingRequest(
                List.of(new LinePricing(VARIANT_1, 50_000, 10_000, 1, 30_000, 10_001)), 0, 0)));
    }

    @Test
    void rejectsEmptyOrInvalidLines() {
        assertInvalid(() -> pricing.calculate(new PricingRequest(List.of(), 0, 0)));
        assertInvalid(() -> pricing.calculate(new PricingRequest(
                List.of(new LinePricing(null, 50_000, 0, 1, 0, 0)), 0, 0)));
        assertInvalid(() -> pricing.calculate(new PricingRequest(
                List.of(new LinePricing(VARIANT_1, 50_000, 0, 0, 0, 0)), 0, 0)));
    }

    @Test
    void rejectsNegativeMoney() {
        assertInvalid(() -> pricing.calculate(new PricingRequest(
                List.of(new LinePricing(VARIANT_1, -1, 0, 1, 0, 0)), 0, 0)));
        assertInvalid(() -> pricing.calculate(new PricingRequest(
                List.of(new LinePricing(VARIANT_1, 50_000, 0, 1, 0, 0)), -1, 0)));
    }

    @Test
    void reportsOverflowWithStableErrorCode() {
        OrderException exception = assertThrows(OrderException.class, () -> pricing.calculate(new PricingRequest(
                List.of(new LinePricing(VARIANT_1, Long.MAX_VALUE, 0, 2, 0, 0)), 0, 0)));

        assertEquals("MONEY_OVERFLOW", exception.getCode());
        assertEquals(422, exception.getStatus().value());
    }

    private void assertInvalid(Runnable action) {
        OrderException exception = assertThrows(OrderException.class, action::run);
        assertEquals("INVALID_PRICE_BREAKDOWN", exception.getCode());
        assertEquals(422, exception.getStatus().value());
    }
}
