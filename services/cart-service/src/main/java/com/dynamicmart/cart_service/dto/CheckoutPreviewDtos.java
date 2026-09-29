package com.dynamicmart.cart_service.dto;

import com.dynamicmart.cart_service.client.IdentityAddressClient.AddressSnapshot;
import com.dynamicmart.cart_service.client.ShippingClient.ShippingQuote;
import com.dynamicmart.cart_service.dto.VoucherDtos.VoucherResponse;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public final class CheckoutPreviewDtos {
    private CheckoutPreviewDtos() { }
    public record PreviewRequest(@NotNull UUID addressId, UUID merchandiseVoucherId, String merchandiseCode,
                                 UUID shippingVoucherId, String shippingCode) { }
    public record PreviewItem(UUID cartItemId, long cartItemVersion, UUID productId, UUID categoryId,
                              UUID variantId, String sku, String productName, String variantName, String imageUrl,
                              UUID directSalePromotionId, int quantity, long listPriceVnd,
                              long directSaleDiscountVnd, long unitPriceVnd, long lineTotalVnd,
                              int weightGrams, int lengthCm, int widthCm, int heightCm) { }
    public record PreviewResponse(UUID cartId, AddressSnapshot address, List<PreviewItem> items,
                                  long listSubtotalVnd, long directSaleDiscountVnd, long itemsSubtotalVnd,
                                  VoucherResponse merchandiseVoucher, VoucherResponse shippingVoucher,
                                  long merchandiseDiscountVnd, long shippingDiscountVnd,
                                  ShippingQuote shippingQuote, long finalTotalVnd) { }
}
