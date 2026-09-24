package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.AddressGateway;
import com.dynamicmart.order_service.client.AddressGateway.AddressSnapshot;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway.TrustedCheckoutItem;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway.TrustedCheckoutSelection;
import com.dynamicmart.order_service.client.VoucherPricingGateway;
import com.dynamicmart.order_service.client.VoucherPricingGateway.AppliedVoucher;
import com.dynamicmart.order_service.client.VoucherPricingGateway.LineDiscount;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherItem;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherPreview;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherPreviewRequest;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.service.CheckoutPricingService.LinePricing;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingBreakdown;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingRequest;
import com.dynamicmart.order_service.service.OrderCreationContextReader.CreationContext;
import com.dynamicmart.order_service.service.OrderCreationContextReader.ItemSnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.MoneySnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.QuoteSnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.VoucherSnapshot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Revalidates all mutable source data after admission and before any reservation side effect. */
@Service
public class OrderCreationRevalidationService {
    private static final String SHIPPING_SCOPE = "SHIPPING_DISCOUNT";

    private final OrderCreationContextReader contexts;
    private final CheckoutSelectionGateway selections;
    private final AddressGateway addresses;
    private final VoucherPricingGateway voucherPricing;
    private final CheckoutPricingService pricing;

    public OrderCreationRevalidationService(
            OrderCreationContextReader contexts,
            CheckoutSelectionGateway selections,
            AddressGateway addresses,
            VoucherPricingGateway voucherPricing,
            CheckoutPricingService pricing) {
        this.contexts = contexts;
        this.selections = selections;
        this.addresses = addresses;
        this.voucherPricing = voucherPricing;
        this.pricing = pricing;
    }

    public ValidatedOrderInput revalidate(UUID customerId, UUID sagaId) {
        CreationContext context = contexts.load(customerId, sagaId);
        TrustedCheckoutSelection currentSelection = loadSelection(context);
        validateSelection(context, currentSelection);

        AddressSnapshot address = addresses.loadOwnedAddress(customerId, context.addressId());
        validateAddress(context.quote(), context.addressId(), address);

        VoucherPreview voucherResult = loadVoucherPreview(context);
        Map<UUID, LineDiscount> discounts = validateVouchers(context, voucherResult);
        PricingBreakdown currentMoney = pricing.calculate(new PricingRequest(
                context.items().stream().map(item -> toPricing(item, discounts.get(item.variantId()))).toList(),
                context.quote().feeVnd(), context.quote().shippingDiscountVnd()));
        validateMoney(context.money(), currentMoney);
        validateQuote(context, address);

        return new ValidatedOrderInput(context, address, voucherResult.vouchers(),
                voucherResult.lineDiscounts(), currentMoney);
    }

    private TrustedCheckoutSelection loadSelection(CreationContext context) {
        if (context.source() == CheckoutSource.CART) {
            return selections.loadSelectedCartItems(context.customerId(), context.cartId());
        }
        if (context.items().size() != 1) {
            throw stale("CHECKOUT_SELECTION_CHANGED", "BUY_NOW không còn đúng một dòng sản phẩm.");
        }
        ItemSnapshot item = context.items().get(0);
        return selections.loadBuyNowItem(context.customerId(), item.variantId(), item.quantity());
    }

    private void validateSelection(CreationContext context, TrustedCheckoutSelection selection) {
        if (selection == null || selection.items() == null || selection.items().isEmpty()) {
            throw stale("CHECKOUT_SELECTION_CHANGED", "Cart/Catalog không còn trả về lựa chọn có thể mua.");
        }
        if (context.source() == CheckoutSource.CART && !Objects.equals(context.cartId(), selection.cartId())) {
            throw stale("CHECKOUT_SELECTION_CHANGED", "Cart nguồn không còn khớp Checkout.");
        }
        if (context.source() == CheckoutSource.BUY_NOW && selection.cartId() != null) {
            throw stale("CHECKOUT_SELECTION_CHANGED", "BUY_NOW không được gắn với Cart.");
        }

        Map<UUID, ItemSnapshot> expected = new HashMap<>();
        for (ItemSnapshot item : context.items()) {
            if (item.variantId() == null || expected.putIfAbsent(item.variantId(), item) != null) {
                throw stale("CHECKOUT_SELECTION_CHANGED", "Snapshot Checkout chứa Variant trùng hoặc không hợp lệ.");
            }
        }
        Map<UUID, TrustedCheckoutItem> actual = new HashMap<>();
        for (TrustedCheckoutItem item : selection.items()) {
            if (item == null || item.variantId() == null || actual.putIfAbsent(item.variantId(), item) != null) {
                throw stale("CHECKOUT_SELECTION_CHANGED", "Cart/Catalog trả về Variant trùng hoặc không hợp lệ.");
            }
        }
        if (!actual.keySet().equals(expected.keySet())) {
            throw stale("CHECKOUT_SELECTION_CHANGED", "Danh sách Variant đã thay đổi sau Preview.");
        }
        for (var entry : expected.entrySet()) {
            if (!samePurchasableSnapshot(entry.getValue(), actual.get(entry.getKey()))) {
                throw stale("CHECKOUT_SELECTION_CHANGED",
                        "Giá, số lượng, phiên bản hoặc thông số Variant đã thay đổi sau Preview.");
            }
        }
    }

    private boolean samePurchasableSnapshot(ItemSnapshot expected, TrustedCheckoutItem actual) {
        return Objects.equals(expected.sourceCartItemId(), actual.sourceCartItemId())
                && Objects.equals(expected.sourceCartItemVersion(), actual.sourceCartItemVersion())
                && Objects.equals(expected.productId(), actual.productId())
                && Objects.equals(expected.variantId(), actual.variantId())
                && Objects.equals(expected.sku(), actual.sku())
                && expected.listPriceVnd() == actual.listPriceVnd()
                && Objects.equals(expected.directSalePromotionId(), actual.directSalePromotionId())
                && expected.directSaleDiscountVnd() == actual.directSaleDiscountVnd()
                && expected.quantity() == actual.quantity()
                && expected.weightGrams() == actual.weightGrams()
                && Objects.equals(expected.lengthCm(), actual.lengthCm())
                && Objects.equals(expected.widthCm(), actual.widthCm())
                && Objects.equals(expected.heightCm(), actual.heightCm());
    }

    private void validateAddress(QuoteSnapshot quote, UUID expectedAddressId, AddressSnapshot address) {
        if (address == null || !Objects.equals(expectedAddressId, address.addressId())
                || isBlank(address.recipientName()) || isBlank(address.phone()) || isBlank(address.addressLine())
                || isBlank(address.provinceName()) || isBlank(address.wardName())
                || !Objects.equals(quote.provinceId(), address.provinceId())
                || !Objects.equals(quote.wardId(), address.wardId())
                || !Objects.equals(quote.provinceName(), address.provinceName())
                || !Objects.equals(quote.wardName(), address.wardName())) {
            throw stale("CHECKOUT_ADDRESS_CHANGED", "Địa chỉ không còn khớp báo giá giao hàng đã Preview.");
        }
    }

    private VoucherPreview loadVoucherPreview(CreationContext context) {
        UUID merchandiseVoucherId = null;
        UUID shippingVoucherId = null;
        for (VoucherSnapshot voucher : context.vouchers()) {
            if (SHIPPING_SCOPE.equals(voucher.scope())) {
                if (shippingVoucherId != null) {
                    throw stale("CHECKOUT_VOUCHER_CHANGED", "Checkout có nhiều hơn một voucher giao hàng.");
                }
                shippingVoucherId = voucher.voucherId();
            } else {
                if (merchandiseVoucherId != null) {
                    throw stale("CHECKOUT_VOUCHER_CHANGED", "Checkout có nhiều hơn một voucher hàng hóa.");
                }
                merchandiseVoucherId = voucher.voucherId();
            }
        }
        if (merchandiseVoucherId == null && shippingVoucherId == null) {
            return VoucherPreview.empty();
        }
        return voucherPricing.preview(new VoucherPreviewRequest(
                context.customerId(), merchandiseVoucherId, shippingVoucherId,
                context.items().stream().map(item -> new VoucherItem(
                        item.productId(), item.variantId(), item.quantity(), item.unitPriceVnd())).toList()));
    }

    private Map<UUID, LineDiscount> validateVouchers(CreationContext context, VoucherPreview result) {
        if (result == null) {
            throw stale("CHECKOUT_VOUCHER_CHANGED", "Voucher Service không trả kết quả revalidation.");
        }
        Map<UUID, VoucherSnapshot> expected = new HashMap<>();
        for (VoucherSnapshot voucher : context.vouchers()) {
            if (voucher.voucherId() == null || expected.putIfAbsent(voucher.voucherId(), voucher) != null) {
                throw stale("CHECKOUT_VOUCHER_CHANGED", "Snapshot voucher bị trùng hoặc không hợp lệ.");
            }
        }
        Map<UUID, AppliedVoucher> actual = new HashMap<>();
        long shippingDiscount = 0;
        for (AppliedVoucher voucher : result.vouchers()) {
            if (voucher == null || voucher.voucherId() == null
                    || isBlank(voucher.discountMethod()) || voucher.eligibleSubtotalVnd() < 0
                    || (voucher.discountValue() != null && voucher.discountValue() < 0)
                    || voucher.discountAmountVnd() < 0 || voucher.shippingDiscountVnd() < 0
                    || actual.putIfAbsent(voucher.voucherId(), voucher) != null) {
                throw stale("CHECKOUT_VOUCHER_CHANGED", "Voucher revalidation bị trùng hoặc không hợp lệ.");
            }
            shippingDiscount = addExact(shippingDiscount, voucher.shippingDiscountVnd());
        }
        if (!actual.keySet().equals(expected.keySet())) {
            throw stale("CHECKOUT_VOUCHER_CHANGED", "Voucher không còn khớp lựa chọn đã Preview.");
        }
        for (var entry : expected.entrySet()) {
            VoucherSnapshot old = entry.getValue();
            AppliedVoucher current = actual.get(entry.getKey());
            if (!Objects.equals(old.voucherCode(), current.voucherCode())
                    || !Objects.equals(old.scope(), current.scope())
                    || old.discountAmountVnd() != current.discountAmountVnd()
                    || old.shippingDiscountVnd() != current.shippingDiscountVnd()) {
                throw stale("CHECKOUT_VOUCHER_CHANGED", "Quyền lợi voucher đã thay đổi sau Preview.");
            }
        }
        if (shippingDiscount != context.quote().shippingDiscountVnd()) {
            throw stale("CHECKOUT_VOUCHER_CHANGED", "Giảm phí giao hàng không còn khớp báo giá.");
        }

        Set<UUID> knownVariants = new HashSet<>();
        context.items().forEach(item -> knownVariants.add(item.variantId()));
        Map<UUID, LineDiscount> discounts = new HashMap<>();
        long productDiscount = 0;
        long orderDiscount = 0;
        for (LineDiscount line : result.lineDiscounts()) {
            if (line == null || line.variantId() == null || !knownVariants.contains(line.variantId())
                    || line.productDiscountVnd() < 0 || line.orderDiscountVnd() < 0
                    || discounts.putIfAbsent(line.variantId(), line) != null) {
                throw stale("CHECKOUT_VOUCHER_CHANGED", "Phân bổ voucher theo dòng không hợp lệ.");
            }
            productDiscount = addExact(productDiscount, line.productDiscountVnd());
            orderDiscount = addExact(orderDiscount, line.orderDiscountVnd());
        }
        if (productDiscount != context.money().productDiscountVnd()
                || orderDiscount != context.money().orderDiscountVnd()) {
            throw stale("CHECKOUT_VOUCHER_CHANGED", "Phân bổ voucher không còn khớp tổng tiền Preview.");
        }
        return discounts;
    }

    private LinePricing toPricing(ItemSnapshot item, LineDiscount discount) {
        return new LinePricing(item.variantId(), item.listPriceVnd(), item.directSaleDiscountVnd(), item.quantity(),
                discount == null ? 0 : discount.productDiscountVnd(),
                discount == null ? 0 : discount.orderDiscountVnd());
    }

    private void validateMoney(MoneySnapshot expected, PricingBreakdown actual) {
        if (expected.itemsListSubtotalVnd() != actual.itemsListSubtotalVnd()
                || expected.directSaleDiscountVnd() != actual.directSaleDiscountVnd()
                || expected.itemsSubtotalVnd() != actual.itemsSubtotalVnd()
                || expected.productDiscountVnd() != actual.productDiscountVnd()
                || expected.orderDiscountVnd() != actual.orderDiscountVnd()
                || expected.shippingFeeVnd() != actual.shippingFeeVnd()
                || expected.shippingDiscountVnd() != actual.shippingDiscountVnd()
                || expected.finalTotalVnd() != actual.finalTotalVnd()) {
            throw stale("CHECKOUT_TOTAL_CHANGED", "Tổng tiền authoritative đã thay đổi sau Preview.");
        }
    }

    private void validateQuote(CreationContext context, AddressSnapshot address) {
        QuoteSnapshot quote = context.quote();
        int totalWeight = 0;
        int length = 0;
        int width = 0;
        int height = 0;
        try {
            for (ItemSnapshot item : context.items()) {
                if (item.lengthCm() == null || item.widthCm() == null || item.heightCm() == null) {
                    throw stale("SHIPPING_QUOTE_STALE", "Snapshot kích thước không đầy đủ.");
                }
                totalWeight = Math.addExact(totalWeight, Math.multiplyExact(item.weightGrams(), item.quantity()));
                length = Math.max(length, item.lengthCm());
                width = Math.max(width, item.widthCm());
                height = Math.addExact(height, Math.multiplyExact(item.heightCm(), item.quantity()));
            }
        } catch (ArithmeticException exception) {
            throw stale("SHIPPING_QUOTE_STALE", "Thông số kiện hàng vượt giới hạn hỗ trợ.");
        }
        if (quote.inputFingerprint() == null || !quote.inputFingerprint().matches("[0-9a-fA-F]{64}")
                || quote.payableFeeVnd() != quote.feeVnd() - quote.shippingDiscountVnd()
                || !Objects.equals(quote.totalWeightGrams(), totalWeight)
                || !Objects.equals(quote.packageLengthCm(), length)
                || !Objects.equals(quote.packageWidthCm(), width)
                || !Objects.equals(quote.packageHeightCm(), height)
                || !Objects.equals(quote.provinceId(), address.provinceId())
                || !Objects.equals(quote.wardId(), address.wardId())) {
            throw stale("SHIPPING_QUOTE_STALE", "Báo giá giao hàng không còn khớp dữ liệu authoritative.");
        }
    }

    private long addExact(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw stale("CHECKOUT_VOUCHER_CHANGED", "Giá trị voucher vượt giới hạn hỗ trợ.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private OrderException stale(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    public record ValidatedOrderInput(
            CreationContext context,
            AddressSnapshot address,
            List<AppliedVoucher> vouchers,
            List<LineDiscount> lineDiscounts,
            PricingBreakdown pricing) {
        public ValidatedOrderInput {
            vouchers = List.copyOf(vouchers);
            lineDiscounts = List.copyOf(lineDiscounts);
        }
    }
}
