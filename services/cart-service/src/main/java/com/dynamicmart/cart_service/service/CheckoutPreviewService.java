package com.dynamicmart.cart_service.service;

import static com.dynamicmart.cart_service.dto.CheckoutPreviewDtos.*;

import com.dynamicmart.cart_service.client.CatalogClient;
import com.dynamicmart.cart_service.client.IdentityAddressClient;
import com.dynamicmart.cart_service.client.ShippingClient;
import com.dynamicmart.cart_service.entity.Cart;
import com.dynamicmart.cart_service.entity.CartItem;
import com.dynamicmart.cart_service.exception.CartException;
import com.dynamicmart.cart_service.repository.CartItemRepository;
import com.dynamicmart.cart_service.repository.CartRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutPreviewService {
    private final CartRepository carts; private final CartItemRepository items; private final CatalogClient catalog;
    private final IdentityAddressClient identity; private final ShippingClient shipping; private final VoucherService vouchers;
    public CheckoutPreviewService(CartRepository carts, CartItemRepository items, CatalogClient catalog,
                                  IdentityAddressClient identity, ShippingClient shipping, VoucherService vouchers) {
        this.carts = carts; this.items = items; this.catalog = catalog; this.identity = identity; this.shipping = shipping; this.vouchers = vouchers;
    }

    @Transactional(readOnly = true)
    public PreviewResponse preview(UUID customerId, PreviewRequest request) {
        Cart cart = carts.findByCustomerIdAndStatus(customerId, "ACTIVE").orElseThrow(() -> invalid("CHECKOUT_CART_EMPTY", "Giỏ hàng đang trống."));
        List<CartItem> selected = items.findAllByCartIdOrderByCreatedAtAsc(cart.getId()).stream().filter(CartItem::isSelected).toList();
        if (selected.isEmpty()) throw invalid("CHECKOUT_CART_EMPTY", "Chưa chọn dòng hàng để mua.");
        Map<UUID, CatalogClient.VariantSnapshot> snapshots = catalog.validate(selected.stream()
                        .map(item -> new CatalogClient.VariantQuantity(item.getVariantId(), item.getQuantity())).toList())
                .stream().collect(Collectors.toMap(CatalogClient.VariantSnapshot::variantId, value -> value));
        List<PreviewItem> previewItems = selected.stream().map(item -> toPreviewItem(item, snapshots.get(item.getVariantId()))).toList();
        long listSubtotal = previewItems.stream().mapToLong(value -> value.listPriceVnd() * value.quantity()).sum();
        long itemsSubtotal = previewItems.stream().mapToLong(PreviewItem::lineTotalVnd).sum();
        var address = identity.requireActive(customerId, request.addressId());
        List<ShippingClient.ShippingItem> shippingItems = previewItems.stream().map(value -> new ShippingClient.ShippingItem(
                value.variantId(), value.quantity(), value.weightGrams(), value.lengthCm(), value.widthCm(), value.heightCm())).toList();

        var merchandise = request.merchandiseVoucherId() == null && blank(request.merchandiseCode()) ? null :
                vouchers.previewForItems(customerId, request.merchandiseVoucherId(), request.merchandiseCode(), itemsSubtotal, 0, discountable(previewItems));
        if (merchandise != null && "SHIPPING_DISCOUNT".equals(merchandise.scope())) throw invalid("VOUCHER_SLOT_INVALID", "Voucher phí giao hàng phải chọn ở ô phí giao hàng.");
        long merchandiseDiscount = merchandise != null && merchandise.eligible() ? merchandise.discountAmountVnd() : 0;

        ShippingClient.ShippingQuote quote = shipping.quote(customerId, address.provinceId(), address.wardId(), shippingItems, 0);
        var shippingVoucher = request.shippingVoucherId() == null && blank(request.shippingCode()) ? null :
                vouchers.previewForItems(customerId, request.shippingVoucherId(), request.shippingCode(), itemsSubtotal, quote.feeVnd(), discountable(previewItems));
        if (shippingVoucher != null && !"SHIPPING_DISCOUNT".equals(shippingVoucher.scope())) throw invalid("VOUCHER_SLOT_INVALID", "Ô phí giao hàng chỉ nhận voucher phí giao hàng.");
        long shippingDiscount = shippingVoucher != null && shippingVoucher.eligible() ? shippingVoucher.discountAmountVnd() : 0;
        if (shippingDiscount > 0) quote = shipping.quote(customerId, address.provinceId(), address.wardId(), shippingItems, shippingDiscount);
        long finalTotal = Math.max(0, itemsSubtotal - merchandiseDiscount + quote.payableFeeVnd());
        return new PreviewResponse(cart.getId(), address, previewItems, listSubtotal, listSubtotal - itemsSubtotal,
                itemsSubtotal, merchandise, shippingVoucher, merchandiseDiscount, shippingDiscount, quote, finalTotal);
    }

    private PreviewItem toPreviewItem(CartItem item, CatalogClient.VariantSnapshot snapshot) {
        if (snapshot == null || !snapshot.purchasable() || snapshot.availableQuantity() < item.getQuantity() || snapshot.price() == null)
            throw invalid("CHECKOUT_VARIANT_UNAVAILABLE", snapshot == null || snapshot.unavailableReason() == null ? "Variant không thể mua." : snapshot.unavailableReason());
        long unitPrice = snapshot.price().effectivePriceVnd();
        return new PreviewItem(item.getId(), item.getVersion(), item.getProductId(), snapshot.categoryId(), item.getVariantId(),
                snapshot.sku(), snapshot.productName(), snapshot.variantName(), snapshot.imageUrl(), snapshot.price().directSalePromotionId(),
                item.getQuantity(), snapshot.price().listPriceVnd(), snapshot.price().directSaleDiscountVnd(),
                unitPrice, Math.multiplyExact(unitPrice, item.getQuantity()), Math.max(1, snapshot.weightGrams()),
                positive(snapshot.lengthCm()), positive(snapshot.widthCm()), positive(snapshot.heightCm()));
    }
    private List<VoucherService.DiscountableItem> discountable(List<PreviewItem> values) { return values.stream().map(value -> new VoucherService.DiscountableItem(value.productId(), value.categoryId(), value.lineTotalVnd())).toList(); }
    private int positive(Integer value) { return value == null || value < 1 ? 1 : value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private CartException invalid(String code, String message) { return new CartException(HttpStatus.UNPROCESSABLE_ENTITY, code, message); }
}
