package com.dynamicmart.order_service.client;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

/** Idempotent reservation boundary implemented by the service that owns voucher quota. */
public interface VoucherReservationGateway {
    Reservation reserve(ReserveVoucherRequest request);

    void release(ReleaseVoucherRequest request);

    record ReserveVoucherRequest(
            UUID operationKey,
            UUID sagaId,
            UUID correlationId,
            UUID customerId,
            UUID checkoutSessionId,
            long shippingFeeVnd,
            Instant reservedUntil,
            List<VoucherBenefit> vouchers,
            List<VoucherLine> lines) {
        public ReserveVoucherRequest {
            vouchers = List.copyOf(vouchers);
            lines = List.copyOf(lines);
        }
    }

    record VoucherBenefit(
            UUID voucherId,
            String code,
            String scope,
            String discountMethod,
            Long discountValue,
            long eligibleSubtotalVnd,
            long discountAmountVnd,
            long shippingDiscountVnd) {
    }

    record VoucherLine(
            UUID productId,
            UUID variantId,
            int quantity,
            long unitPriceVnd,
            long productDiscountVnd,
            long orderDiscountVnd) {
    }

    record Reservation(UUID reservationId) {
    }

    record ReleaseVoucherRequest(
            UUID operationKey,
            UUID sagaId,
            UUID correlationId,
            UUID reservationId,
            String reason) {
    }
}
