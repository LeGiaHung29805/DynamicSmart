package com.dynamicmart.order_service.dto.response;

import com.dynamicmart.order_service.entity.OrderActorType;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        OrderStatus status,
        PaymentTiming paymentTiming,
        PaymentMethod paymentMethod,
        MoneyBreakdown money,
        Instant paymentDueAt,
        Instant paymentSucceededAt,
        Instant shipmentDeliveredAt,
        String cancelReason,
        Instant cancelledAt,
        Instant confirmedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        List<String> availableActions,
        List<Item> items,
        Address address,
        List<Voucher> vouchers,
        Shipping shipping,
        List<TimelineEntry> timeline) {

    public record MoneyBreakdown(
            long itemsListSubtotalVnd,
            long directSaleDiscountVnd,
            long itemsSubtotalVnd,
            long productDiscountVnd,
            long orderDiscountVnd,
            long shippingFeeVnd,
            long shippingDiscountVnd,
            long finalTotalVnd,
            String currency) {
    }

    public record Item(
            UUID itemId,
            UUID productId,
            UUID variantId,
            String sku,
            String productName,
            String variantName,
            String imageUrl,
            long listPriceVnd,
            UUID directSalePromotionId,
            long directSaleDiscountVnd,
            long unitPriceVnd,
            int quantity,
            long productDiscountVnd,
            long orderDiscountVnd,
            long lineTotalVnd,
            int weightGrams) {
    }

    public record Address(
            String recipientName,
            String phone,
            String addressLine,
            int provinceId,
            int wardId,
            String provinceName,
            String wardName) {
    }

    public record Voucher(
            UUID voucherId,
            String voucherCode,
            String scope,
            String discountMethod,
            Long discountValue,
            long eligibleSubtotalVnd,
            long discountAmountVnd,
            long shippingDiscountVnd) {
    }

    public record Shipping(
            UUID quoteId,
            String provider,
            int serviceId,
            String serviceName,
            long feeVnd,
            long shippingDiscountVnd,
            long payableFeeVnd,
            Instant eta,
            String etaText,
            Integer totalWeightGrams,
            Integer packageLengthCm,
            Integer packageWidthCm,
            Integer packageHeightCm,
            Integer toProvinceId,
            Integer toWardId,
            String toProvinceName,
            String toWardName,
            Instant quotedAt) {
    }

    public record TimelineEntry(
            UUID historyId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            OrderActorType actorType,
            UUID actorId,
            String reason,
            UUID correlationId,
            Instant createdAt) {
    }
}
