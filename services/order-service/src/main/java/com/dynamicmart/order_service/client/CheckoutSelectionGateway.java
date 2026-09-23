package com.dynamicmart.order_service.client;

import java.util.List;
import java.util.UUID;

/**
 * Boundary for authoritative Cart and Catalog data used to open a Checkout Session.
 * Implementations must never trust browser-supplied prices, stock, or product metadata.
 */
public interface CheckoutSelectionGateway {
    TrustedCheckoutSelection loadSelectedCartItems(UUID customerId, UUID cartId);

    TrustedCheckoutSelection loadBuyNowItem(UUID customerId, UUID variantId, int quantity);

    record TrustedCheckoutSelection(UUID cartId, List<TrustedCheckoutItem> items) {
        public TrustedCheckoutSelection {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    record TrustedCheckoutItem(
            UUID sourceCartItemId,
            Long sourceCartItemVersion,
            UUID productId,
            UUID variantId,
            String sku,
            String productName,
            String variantName,
            String imageUrl,
            long listPriceVnd,
            UUID directSalePromotionId,
            long directSaleDiscountVnd,
            int quantity,
            int weightGrams,
            int lengthCm,
            int widthCm,
            int heightCm) {
    }
}
