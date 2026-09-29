package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.exception.OrderException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShippingPackageCalculatorTests {
    private final ShippingPackageCalculator calculator = new ShippingPackageCalculator();

    @Test
    void mirrorsPaymentPackageAggregationRule() {
        var first = item(2, 300, 20, 10, 5);
        var second = item(1, 500, 15, 12, 8);

        var result = calculator.calculate(List.of(first, second));

        assertEquals(1_100, result.totalWeightGrams());
        assertEquals(20, result.lengthCm());
        assertEquals(12, result.widthCm());
        assertEquals(18, result.heightCm());
    }

    @Test
    void rejectsLegacySnapshotWithoutDimensions() {
        var item = item(1, 300, 20, 10, 5);
        item.setLengthCm(null);

        OrderException exception = assertThrows(OrderException.class, () -> calculator.calculate(List.of(item)));

        assertEquals("INVALID_SHIPPING_PACKAGE", exception.getCode());
    }

    @Test
    void reportsIntegerOverflowWithStableCode() {
        var item = item(Integer.MAX_VALUE, 2, 20, 10, 5);

        OrderException exception = assertThrows(OrderException.class, () -> calculator.calculate(List.of(item)));

        assertEquals("SHIPPING_PACKAGE_OVERFLOW", exception.getCode());
    }

    private CheckoutSessionItem item(int quantity, int weight, int length, int width, int height) {
        return CheckoutSessionItem.create(
                UUID.randomUUID(), UUID.randomUUID(), null, null, UUID.randomUUID(), UUID.randomUUID(),
                "SKU", "Product", null, null, 100_000, null, 0,
                quantity, weight, length, width, height, Instant.parse("2026-09-23T09:00:00Z"));
    }
}
