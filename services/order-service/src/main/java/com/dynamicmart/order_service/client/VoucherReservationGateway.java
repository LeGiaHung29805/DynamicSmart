package com.dynamicmart.order_service.client;

import java.util.List;
import java.util.UUID;

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
