package com.dynamicmart.order_service.dto.response;

import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutPreviewResponse(
        UUID checkoutSessionId,
        UUID addressId,
        List<VoucherResponse> vouchers,
        ShippingQuoteResponse shipping,
        MoneyBreakdown money,
        PaymentTiming paymentTiming,
        PaymentMethod paymentMethod) {
    public CheckoutPreviewResponse {
        vouchers = List.copyOf(vouchers);
    }

    public record VoucherResponse(
            UUID voucherId,
            String voucherCode,
            String scope,
            long discountAmountVnd,
            long shippingDiscountVnd) {
    }

    public record ShippingQuoteResponse(
            UUID quoteId,
            long feeVnd,
            long shippingDiscountVnd,
            long payableFeeVnd,
            int serviceId,
            String serviceName,
            String eta,
            Instant expiresAt) {
    }

    public record MoneyBreakdown(
            long itemsListSubtotalVnd,
            long directSaleDiscountVnd,
            long itemsSubtotalVnd,
            long productDiscountVnd,
            long orderDiscountVnd,
            long shippingFeeVnd,
            long shippingDiscountVnd,
            long finalTotalVnd) {
    }
}
