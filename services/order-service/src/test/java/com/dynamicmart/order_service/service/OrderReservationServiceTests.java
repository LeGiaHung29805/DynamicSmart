package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.AddressGateway.AddressSnapshot;
import com.dynamicmart.order_service.client.InventoryReservationGateway;
import com.dynamicmart.order_service.client.VoucherPricingGateway.AppliedVoucher;
import com.dynamicmart.order_service.client.VoucherPricingGateway.LineDiscount;
import com.dynamicmart.order_service.client.VoucherReservationGateway;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingBreakdown;
import com.dynamicmart.order_service.service.OrderCreationContextReader.CreationContext;
import com.dynamicmart.order_service.service.OrderCreationContextReader.ItemSnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.MoneySnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.QuoteSnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.VoucherSnapshot;
import com.dynamicmart.order_service.service.OrderCreationRevalidationService.ValidatedOrderInput;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class OrderReservationServiceTests {
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID VOUCHER_RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID INVENTORY_RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void reservesVoucherThenInventoryAndPersistsEachCheckpoint() {
        Fixture fixture = readyFixture();
        InOrder order = inOrder(fixture.vouchers, fixture.inventory, fixture.checkpoints);

        var result = fixture.service.reserve(validatedInput(true));

        assertEquals(VOUCHER_RESERVATION_ID, result.voucherReservationId());
        assertEquals(INVENTORY_RESERVATION_ID, result.inventoryReservationId());
        order.verify(fixture.vouchers).reserve(any());
        order.verify(fixture.checkpoints).markVoucherReserved(SAGA_ID, VOUCHER_RESERVATION_ID);
        order.verify(fixture.inventory).reserve(any());
        order.verify(fixture.checkpoints).markInventoryReserved(SAGA_ID, INVENTORY_RESERVATION_ID);
    }

    @Test
    void skipsVoucherRemoteCallWhenCheckoutHasNoVoucher() {
        Fixture fixture = readyFixture();

        var result = fixture.service.reserve(validatedInput(false));

        assertNull(result.voucherReservationId());
        verify(fixture.vouchers, never()).reserve(any());
        verify(fixture.checkpoints).markVoucherReserved(SAGA_ID, null);
        verify(fixture.inventory).reserve(any());
    }

    @Test
    void voucherReserveFailureLeavesSagaAtReplayableAdmissionCheckpoint() {
        Fixture fixture = readyFixture();
        OrderException failure = new OrderException(
                HttpStatus.SERVICE_UNAVAILABLE, "VOUCHER_UNAVAILABLE", "Voucher unavailable");
        when(fixture.vouchers.reserve(any())).thenThrow(failure);

        OrderException thrown = assertThrows(OrderException.class,
                () -> fixture.service.reserve(validatedInput(true)));

        assertEquals(failure, thrown);
        verify(fixture.checkpoints, never()).markVoucherReserved(any(), any());
        verify(fixture.checkpoints, never()).beginCompensation(any(), any(), any());
        verify(fixture.inventory, never()).reserve(any());
    }

    @Test
    void inventoryFailureReleasesVoucherAndCompletesCompensation() {
        Fixture fixture = readyFixture();
        OrderException failure = new OrderException(
                HttpStatus.CONFLICT, "OUT_OF_STOCK", "Inventory changed");
        when(fixture.inventory.reserve(any())).thenThrow(failure);

        OrderException thrown = assertThrows(OrderException.class,
                () -> fixture.service.reserve(validatedInput(true)));

        assertEquals(failure, thrown);
        verify(fixture.checkpoints).beginCompensation(SAGA_ID, "OUT_OF_STOCK", "Inventory changed");
        verify(fixture.vouchers).release(any());
        verify(fixture.inventory, never()).release(any());
        verify(fixture.checkpoints).markCompensated(SAGA_ID);
    }

    @Test
    void checkpointFailureAfterInventoryReserveCompensatesInReverseOrderWithStableKeys() {
        Fixture fixture = readyFixture();
        RuntimeException failure = new IllegalStateException("checkpoint unavailable");
        Mockito.doThrow(failure).when(fixture.checkpoints)
                .markInventoryReserved(SAGA_ID, INVENTORY_RESERVATION_ID);
        InOrder order = inOrder(fixture.inventory, fixture.vouchers, fixture.checkpoints);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> fixture.service.reserve(validatedInput(true)));

        assertEquals(failure, thrown);
        order.verify(fixture.checkpoints).beginCompensation(
                SAGA_ID, "IllegalStateException", "checkpoint unavailable");
        order.verify(fixture.inventory).release(any());
        order.verify(fixture.vouchers).release(any());
        order.verify(fixture.checkpoints).markCompensated(SAGA_ID);

        ArgumentCaptor<InventoryReservationGateway.ReleaseInventoryRequest> inventoryRelease =
                ArgumentCaptor.forClass(InventoryReservationGateway.ReleaseInventoryRequest.class);
        verify(fixture.inventory).release(inventoryRelease.capture());
        assertEquals(OrderReservationService.operationKey(SAGA_ID, "INVENTORY_RELEASE"),
                inventoryRelease.getValue().operationKey());
    }

    @Test
    void compensationFailureIsRecordedWithoutHidingOriginalFailure() {
        Fixture fixture = readyFixture();
        OrderException original = new OrderException(HttpStatus.CONFLICT, "OUT_OF_STOCK", "out");
        IllegalStateException releaseFailure = new IllegalStateException("release unavailable");
        when(fixture.inventory.reserve(any())).thenThrow(original);
        Mockito.doThrow(releaseFailure).when(fixture.vouchers).release(any());

        OrderException thrown = assertThrows(OrderException.class,
                () -> fixture.service.reserve(validatedInput(true)));

        assertEquals(original, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals(releaseFailure, thrown.getSuppressed()[0]);
        verify(fixture.checkpoints).recordCompensationFailure(SAGA_ID, releaseFailure);
        verify(fixture.checkpoints, never()).markCompensated(any());
    }

    private Fixture readyFixture() {
        VoucherReservationGateway vouchers = Mockito.mock(VoucherReservationGateway.class);
        InventoryReservationGateway inventory = Mockito.mock(InventoryReservationGateway.class);
        OrderSagaCheckpointService checkpoints = Mockito.mock(OrderSagaCheckpointService.class);
        when(vouchers.reserve(any())).thenReturn(new VoucherReservationGateway.Reservation(VOUCHER_RESERVATION_ID));
        when(inventory.reserve(any())).thenReturn(new InventoryReservationGateway.Reservation(INVENTORY_RESERVATION_ID));
        return new Fixture(vouchers, inventory, checkpoints,
                new OrderReservationService(vouchers, inventory, checkpoints));
    }

    private ValidatedOrderInput validatedInput(boolean withVoucher) {
        UUID customerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID voucherId = UUID.randomUUID();
        ItemSnapshot item = new ItemSnapshot(
                UUID.randomUUID(), 1L, productId, variantId, "SKU-1", "Sản phẩm", "Đỏ", null,
                100_000, null, 0, 100_000, 2, 200, 20, 10, 5);
        List<VoucherSnapshot> voucherSnapshots = withVoucher
                ? List.of(new VoucherSnapshot(voucherId, "SAVE10", "ORDER_DISCOUNT", 10_000, 0))
                : List.of();
        CreationContext context = new CreationContext(
                SAGA_ID, CORRELATION_ID, sessionId, customerId, CheckoutSource.CART, UUID.randomUUID(),
                UUID.randomUUID(), PaymentTiming.PREPAID, PaymentMethod.VNPAY,
                new MoneySnapshot(200_000, 0, 200_000, 0, withVoucher ? 10_000 : 0,
                        20_000, 0, withVoucher ? 210_000 : 220_000),
                List.of(item), voucherSnapshots,
                new QuoteSnapshot(
                        UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64), 20_000, 0, 20_000,
                        1, "GHN", "1 ngày", 400, 20, 10, 10, 1, 2,
                        "Hà Nội", "Phường A", Instant.parse("2026-09-24T05:00:00Z")));
        List<AppliedVoucher> vouchers = withVoucher
                ? List.of(new AppliedVoucher(voucherId, "SAVE10", "ORDER_DISCOUNT", 10_000, 0))
                : List.of();
        List<LineDiscount> lines = withVoucher
                ? List.of(new LineDiscount(variantId, 0, 10_000))
                : List.of();
        long orderDiscount = withVoucher ? 10_000 : 0;
        return new ValidatedOrderInput(
                context,
                new AddressSnapshot(context.addressId(), "Hiếu", "0900000000", "1 Đường A",
                        1, 2, "Hà Nội", "Phường A"),
                vouchers,
                lines,
                new PricingBreakdown(200_000, 0, 200_000, 0, orderDiscount,
                        20_000, 0, 220_000 - orderDiscount));
    }

    private record Fixture(
            VoucherReservationGateway vouchers,
            InventoryReservationGateway inventory,
            OrderSagaCheckpointService checkpoints,
            OrderReservationService service) {
    }
}
