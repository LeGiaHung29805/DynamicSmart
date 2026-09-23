package com.dynamicmart.order_service.client;

import java.util.List;
import java.util.UUID;

public interface VoucherPricingGateway {
    VoucherPreview preview(VoucherPreviewRequest request);

    record VoucherPreviewRequest(
            UUID customerId,
            UUID merchandiseVoucherId,
            UUID shippingVoucherId,
            List<VoucherItem> items) {
        public VoucherPreviewRequest {
            items = List.copyOf(items);
        }
    }

    record VoucherItem(UUID productId, UUID variantId, int quantity, long unitPriceVnd) {
    }

    record VoucherPreview(List<AppliedVoucher> vouchers, List<LineDiscount> lineDiscounts) {
        public VoucherPreview {
            vouchers = vouchers == null ? List.of() : List.copyOf(vouchers);
            lineDiscounts = lineDiscounts == null ? List.of() : List.copyOf(lineDiscounts);
        }

        public static VoucherPreview empty() {
            return new VoucherPreview(List.of(), List.of());
        }
    }

    record AppliedVoucher(
            UUID voucherId,
            String voucherCode,
            String scope,
            long discountAmountVnd,
            long shippingDiscountVnd) {
    }

    record LineDiscount(UUID variantId, long productDiscountVnd, long orderDiscountVnd) {
    }
}
