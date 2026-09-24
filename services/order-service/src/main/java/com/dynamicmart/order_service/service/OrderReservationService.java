package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.InventoryReservationGateway;
import com.dynamicmart.order_service.client.InventoryReservationGateway.InventoryLine;
import com.dynamicmart.order_service.client.InventoryReservationGateway.ReleaseInventoryRequest;
import com.dynamicmart.order_service.client.InventoryReservationGateway.ReserveInventoryRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway;
import com.dynamicmart.order_service.client.VoucherReservationGateway.ReleaseVoucherRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway.ReserveVoucherRequest;
import com.dynamicmart.order_service.client.VoucherReservationGateway.VoucherBenefit;
import com.dynamicmart.order_service.client.VoucherReservationGateway.VoucherLine;
import com.dynamicmart.order_service.client.VoucherPricingGateway.LineDiscount;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.service.OrderCreationContextReader.CreationContext;
import com.dynamicmart.order_service.service.OrderCreationRevalidationService.ValidatedOrderInput;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Runs remote reservations without a database transaction and compensates in reverse order on failure. */
@Service
public class OrderReservationService {
    private final VoucherReservationGateway vouchers;
    private final InventoryReservationGateway inventory;
    private final OrderSagaCheckpointService checkpoints;

    public OrderReservationService(
            VoucherReservationGateway vouchers,
            InventoryReservationGateway inventory,
            OrderSagaCheckpointService checkpoints) {
        this.vouchers = vouchers;
        this.inventory = inventory;
        this.checkpoints = checkpoints;
    }

    public ReservationResult reserve(ValidatedOrderInput input) {
        CreationContext context = input.context();
        UUID voucherReservationId = null;
        UUID inventoryReservationId = null;
        boolean voucherCheckpointed = false;
        try {
            if (!input.vouchers().isEmpty()) {
                var response = vouchers.reserve(voucherRequest(input));
                voucherReservationId = requireReservationId(
                        response == null ? null : response.reservationId(), "VOUCHER_RESERVATION_INVALID");
            }
            checkpoints.markVoucherReserved(context.sagaId(), voucherReservationId);
            voucherCheckpointed = true;

            var inventoryResponse = inventory.reserve(inventoryRequest(input));
            inventoryReservationId = requireReservationId(
                    inventoryResponse == null ? null : inventoryResponse.reservationId(),
                    "INVENTORY_RESERVATION_INVALID");
            checkpoints.markInventoryReserved(context.sagaId(), inventoryReservationId);
            return new ReservationResult(voucherReservationId, inventoryReservationId);
        } catch (RuntimeException failure) {
            if (voucherCheckpointed || voucherReservationId != null || inventoryReservationId != null) {
                compensate(context, voucherReservationId, inventoryReservationId, failure);
            }
            throw failure;
        }
    }

    private ReserveVoucherRequest voucherRequest(ValidatedOrderInput input) {
        CreationContext context = input.context();
        Map<UUID, LineDiscount> discounts = input.lineDiscounts().stream()
                .collect(Collectors.toMap(
                        LineDiscount::variantId,
                        line -> line));
        return new ReserveVoucherRequest(
                operationKey(context.sagaId(), "VOUCHER_RESERVE"), context.sagaId(), context.correlationId(),
                context.customerId(), context.checkoutSessionId(),
                input.vouchers().stream().map(voucher -> new VoucherBenefit(
                        voucher.voucherId(), voucher.voucherCode(), voucher.scope(),
                        voucher.discountAmountVnd(), voucher.shippingDiscountVnd())).toList(),
                context.items().stream().map(item -> {
                    LineDiscount discount = discounts.get(item.variantId());
                    return new VoucherLine(
                            item.productId(), item.variantId(), item.quantity(), item.unitPriceVnd(),
                            discount == null ? 0 : discount.productDiscountVnd(),
                            discount == null ? 0 : discount.orderDiscountVnd());
                }).toList());
    }

    private ReserveInventoryRequest inventoryRequest(ValidatedOrderInput input) {
        CreationContext context = input.context();
        return new ReserveInventoryRequest(
                operationKey(context.sagaId(), "INVENTORY_RESERVE"), context.sagaId(), context.correlationId(),
                context.customerId(), context.checkoutSessionId(),
                context.items().stream().map(item ->
                        new InventoryLine(item.productId(), item.variantId(), item.quantity())).toList());
    }

    private void compensate(
            CreationContext context,
            UUID voucherReservationId,
            UUID inventoryReservationId,
            RuntimeException originalFailure) {
        String code = originalFailure instanceof OrderException orderFailure
                ? orderFailure.getCode()
                : originalFailure.getClass().getSimpleName();
        checkpoints.beginCompensation(context.sagaId(), code, originalFailure.getMessage());
        try {
            if (inventoryReservationId != null) {
                inventory.release(new ReleaseInventoryRequest(
                        operationKey(context.sagaId(), "INVENTORY_RELEASE"), context.sagaId(),
                        context.correlationId(), inventoryReservationId, code));
            }
            if (voucherReservationId != null) {
                vouchers.release(new ReleaseVoucherRequest(
                        operationKey(context.sagaId(), "VOUCHER_RELEASE"), context.sagaId(),
                        context.correlationId(), voucherReservationId, code));
            }
            checkpoints.markCompensated(context.sagaId());
        } catch (RuntimeException compensationFailure) {
            checkpoints.recordCompensationFailure(context.sagaId(), compensationFailure);
            originalFailure.addSuppressed(compensationFailure);
        }
    }

    static UUID operationKey(UUID sagaId, String operation) {
        return UUID.nameUUIDFromBytes(
                ("ORDER_SAGA|" + sagaId + "|" + operation).getBytes(StandardCharsets.UTF_8));
    }

    private UUID requireReservationId(UUID reservationId, String code) {
        if (reservationId == null) {
            throw new OrderException(HttpStatus.BAD_GATEWAY, code,
                    "Service phụ thuộc không trả reservation ID hợp lệ.");
        }
        return reservationId;
    }

    public record ReservationResult(UUID voucherReservationId, UUID inventoryReservationId) {
    }
}
