package com.dynamicmart.cart_service.service;

import static com.dynamicmart.cart_service.dto.InternalCartDtos.*;

import com.dynamicmart.cart_service.client.CatalogClient;
import com.dynamicmart.cart_service.entity.Cart;
import com.dynamicmart.cart_service.exception.CartException;
import com.dynamicmart.cart_service.repository.CartItemRepository;
import com.dynamicmart.cart_service.repository.CartRepository;
import com.dynamicmart.cart_service.repository.VoucherReservationRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCheckoutIntegrationService {
    private final CartRepository carts; private final CartItemRepository items; private final CatalogClient catalog;
    private final VoucherService vouchers; private final VoucherReservationRepository reservations;
    public OrderCheckoutIntegrationService(CartRepository carts, CartItemRepository items, CatalogClient catalog,
                                           VoucherService vouchers, VoucherReservationRepository reservations) {
        this.carts = carts; this.items = items; this.catalog = catalog; this.vouchers = vouchers; this.reservations = reservations;
    }

    @Transactional(readOnly = true)
    public CheckoutSelectionResponse selected(UUID customerId, UUID cartId) {
        Cart cart = carts.findByCustomerIdAndStatus(customerId, "ACTIVE")
                .filter(value -> value.getId().equals(cartId)).orElseThrow(() -> invalid("CHECKOUT_CART_NOT_FOUND", "Không tìm thấy giỏ hàng đang hoạt động."));
        List<CheckoutSelectionItem> selected = items.findAllByCartIdOrderByCreatedAtAsc(cart.getId()).stream()
                .filter(value -> value.isSelected())
                .map(value -> new CheckoutSelectionItem(value.getId(), value.getVersion(), value.getProductId(),
                        value.getVariantId(), value.getQuantity())).toList();
        if (selected.isEmpty()) throw invalid("CHECKOUT_CART_EMPTY", "Chưa chọn dòng hàng để mua.");
        return new CheckoutSelectionResponse(cart.getId(), selected);
    }

    @Transactional(readOnly = true)
    public VoucherPricingResponse preview(VoucherPricingRequest request) {
        List<EnrichedLine> lines = enrich(request.items());
        long subtotal = lines.stream().mapToLong(EnrichedLine::lineTotal).sum();
        List<AppliedVoucher> applied = new ArrayList<>(); List<LineDiscount> discounts = new ArrayList<>();
        if (request.merchandiseVoucherId() != null) {
            var result = vouchers.previewForItems(request.customerId(), request.merchandiseVoucherId(), null,
                    subtotal, request.shippingFeeVnd(), discountable(lines));
            requireEligible(result.eligible(), result.ineligibleReason());
            long eligible = eligibleSubtotal(result.scope(), result.productIds(), result.categoryIds(), lines, subtotal);
            applied.add(applied(result, eligible));
            discounts.addAll(allocate(result.scope(), result.discountAmountVnd(), result.productIds(), result.categoryIds(), lines));
        }
        if (request.shippingVoucherId() != null) {
            var result = vouchers.previewForItems(request.customerId(), request.shippingVoucherId(), null,
                    subtotal, request.shippingFeeVnd(), discountable(lines));
            requireEligible(result.eligible(), result.ineligibleReason());
            if (!"SHIPPING_DISCOUNT".equals(result.scope())) throw invalid("VOUCHER_SLOT_INVALID", "Voucher không thuộc ô phí giao hàng.");
            applied.add(applied(result, subtotal));
        }
        return new VoucherPricingResponse(applied, discounts);
    }

    @Transactional
    public VoucherGroupResponse reserveGroup(ReserveVoucherGroupRequest request) {
        VoucherPricingResponse current = preview(new VoucherPricingRequest(request.customerId(),
                merchandise(request.vouchers()), shipping(request.vouchers()), request.shippingFeeVnd(),
                request.lines().stream().map(value -> new PricingItem(value.productId(), value.variantId(), value.quantity(), value.unitPriceVnd())).toList()));
        Map<UUID, AppliedVoucher> actual = current.vouchers().stream().collect(Collectors.toMap(AppliedVoucher::voucherId, value -> value));
        long subtotal = request.lines().stream().mapToLong(value -> Math.multiplyExact(value.unitPriceVnd(), value.quantity())).sum();
        List<EnrichedLine> lines = enrich(request.lines().stream().map(value -> new PricingItem(value.productId(), value.variantId(), value.quantity(), value.unitPriceVnd())).toList());
        Set<UUID> productIds = lines.stream().map(EnrichedLine::productId).collect(Collectors.toSet());
        Set<UUID> categoryIds = lines.stream().map(EnrichedLine::categoryId).collect(Collectors.toSet());
        for (ReservationBenefit expected : request.vouchers()) {
            AppliedVoucher value = actual.get(expected.voucherId());
            if (value == null || value.discountAmountVnd() != expected.discountAmountVnd()
                    || value.shippingDiscountVnd() != expected.shippingDiscountVnd())
                throw new CartException(HttpStatus.CONFLICT, "VOUCHER_BENEFIT_CHANGED", "Quyền lợi voucher đã thay đổi sau Preview.");
            vouchers.reserve(new ReserveVoucherRequest(expected.voucherId(), request.customerId(), request.sagaId(),
                    subtotal, value.eligibleSubtotalVnd(), request.shippingFeeVnd(), productIds, categoryIds, request.reservedUntil()));
        }
        return new VoucherGroupResponse(request.sagaId());
    }

    @Transactional
    public VoucherGroupResponse releaseGroup(UUID groupId, String reason) {
        reservations.findAllByCheckoutSessionId(groupId).forEach(value -> vouchers.release(value.getId(), reason));
        return new VoucherGroupResponse(groupId);
    }

    @Transactional
    public VoucherGroupResponse consumeGroup(UUID groupId, UUID orderId) {
        reservations.findAllByCheckoutSessionId(groupId).forEach(value -> vouchers.consume(value.getId(), orderId));
        return new VoucherGroupResponse(groupId);
    }

    private List<EnrichedLine> enrich(List<PricingItem> values) {
        Map<UUID, CatalogClient.VariantSnapshot> snapshots = catalog.validate(values.stream()
                .map(value -> new CatalogClient.VariantQuantity(value.variantId(), value.quantity())).toList())
                .stream().collect(Collectors.toMap(CatalogClient.VariantSnapshot::variantId, value -> value));
        return values.stream().map(value -> {
            var snapshot = snapshots.get(value.variantId());
            if (snapshot == null || !snapshot.purchasable() || snapshot.availableQuantity() < value.quantity())
                throw invalid("CHECKOUT_VARIANT_UNAVAILABLE", "Variant không còn đủ điều kiện mua.");
            long trustedUnitPrice = snapshot.price().effectivePriceVnd();
            if (trustedUnitPrice != value.unitPriceVnd()) throw new CartException(HttpStatus.CONFLICT, "CHECKOUT_PRICE_CHANGED", "Giá Variant đã thay đổi.");
            return new EnrichedLine(value.productId(), snapshot.categoryId(), value.variantId(), value.quantity(), trustedUnitPrice, Math.multiplyExact(trustedUnitPrice, value.quantity()));
        }).toList();
    }
    private List<VoucherService.DiscountableItem> discountable(List<EnrichedLine> lines) { return lines.stream().map(value -> new VoucherService.DiscountableItem(value.productId(), value.categoryId(), value.lineTotal())).toList(); }
    private long eligibleSubtotal(String scope, Set<UUID> products, Set<UUID> categories, List<EnrichedLine> lines, long subtotal) {
        return switch (scope) {
            case "PRODUCT_DISCOUNT", "PRODUCT_LIST_DISCOUNT" -> lines.stream().filter(value -> products.contains(value.productId())).mapToLong(EnrichedLine::lineTotal).sum();
            case "CATEGORY_DISCOUNT" -> lines.stream().filter(value -> categories.contains(value.categoryId())).mapToLong(EnrichedLine::lineTotal).sum();
            default -> subtotal;
        };
    }
    private List<LineDiscount> allocate(String scope, long total, Set<UUID> products, Set<UUID> categories, List<EnrichedLine> lines) {
        List<EnrichedLine> eligible = lines.stream().filter(value -> switch (scope) {
            case "PRODUCT_DISCOUNT", "PRODUCT_LIST_DISCOUNT" -> products.contains(value.productId());
            case "CATEGORY_DISCOUNT" -> categories.contains(value.categoryId()); default -> true;
        }).toList();
        Map<UUID, LineDiscount> result = new LinkedHashMap<>(); long remaining = total;
        for (EnrichedLine line : eligible) {
            long amount = Math.min(remaining, line.lineTotal()); remaining -= amount;
            boolean product = !"ORDER_DISCOUNT".equals(scope);
            result.put(line.variantId(), new LineDiscount(line.variantId(), product ? amount : 0, product ? 0 : amount));
        }
        return List.copyOf(result.values());
    }
    private AppliedVoucher applied(com.dynamicmart.cart_service.dto.VoucherDtos.VoucherResponse value, long eligible) {
        Long discountValue = "FIXED_AMOUNT".equals(value.discountMethod()) ? value.fixedDiscountVnd()
                : value.discountRateBps() == null ? null : value.discountRateBps().longValue();
        return new AppliedVoucher(value.id(), value.code(), value.scope(), value.discountMethod(), discountValue,
                eligible, "SHIPPING_DISCOUNT".equals(value.scope()) ? 0 : value.discountAmountVnd(),
                "SHIPPING_DISCOUNT".equals(value.scope()) ? value.discountAmountVnd() : 0);
    }
    private UUID merchandise(List<ReservationBenefit> values) { return values.stream().filter(value -> !"SHIPPING_DISCOUNT".equals(value.scope())).map(ReservationBenefit::voucherId).findFirst().orElse(null); }
    private UUID shipping(List<ReservationBenefit> values) { return values.stream().filter(value -> "SHIPPING_DISCOUNT".equals(value.scope())).map(ReservationBenefit::voucherId).findFirst().orElse(null); }
    private void requireEligible(boolean eligible, String reason) { if (!eligible) throw invalid("VOUCHER_INELIGIBLE", reason == null ? "Voucher không còn hợp lệ." : reason); }
    private CartException invalid(String code, String message) { return new CartException(HttpStatus.UNPROCESSABLE_ENTITY, code, message); }
    private record EnrichedLine(UUID productId, UUID categoryId, UUID variantId, int quantity, long unitPrice, long lineTotal) { }
}
