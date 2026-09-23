package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.AddressGateway;
import com.dynamicmart.order_service.client.AddressGateway.AddressSnapshot;
import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.client.PaymentClient.ShippingItemRequest;
import com.dynamicmart.order_service.client.PaymentClient.ShippingQuoteRequest;
import com.dynamicmart.order_service.client.VoucherPricingGateway;
import com.dynamicmart.order_service.client.VoucherPricingGateway.AppliedVoucher;
import com.dynamicmart.order_service.client.VoucherPricingGateway.LineDiscount;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherItem;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherPreview;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherPreviewRequest;
import com.dynamicmart.order_service.dto.request.CheckoutPreviewRequest;
import com.dynamicmart.order_service.dto.response.CheckoutPreviewResponse;
import com.dynamicmart.order_service.dto.response.CheckoutPreviewResponse.MoneyBreakdown;
import com.dynamicmart.order_service.dto.response.CheckoutPreviewResponse.ShippingQuoteResponse;
import com.dynamicmart.order_service.dto.response.CheckoutPreviewResponse.VoucherResponse;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.service.CheckoutPreviewPersistenceService.PersistedPreview;
import com.dynamicmart.order_service.service.CheckoutPreviewPersistenceService.PreviewCommit;
import com.dynamicmart.order_service.service.CheckoutPricingService.LinePricing;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingBreakdown;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingRequest;
import com.dynamicmart.order_service.service.ShippingPackageCalculator.PackageMetrics;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CheckoutPreviewService {
    private static final String SHIPPING_SCOPE = "SHIPPING_DISCOUNT";

    private final CheckoutSessionRepository sessions;
    private final CheckoutSessionItemRepository sessionItems;
    private final AddressGateway addresses;
    private final VoucherPricingGateway voucherPricing;
    private final PaymentClient payments;
    private final CheckoutPricingService pricing;
    private final ShippingPackageCalculator packages;
    private final CheckoutPreviewPersistenceService persistence;
    private final Clock clock;

    public CheckoutPreviewService(
            CheckoutSessionRepository sessions,
            CheckoutSessionItemRepository sessionItems,
            AddressGateway addresses,
            VoucherPricingGateway voucherPricing,
            PaymentClient payments,
            CheckoutPricingService pricing,
            ShippingPackageCalculator packages,
            CheckoutPreviewPersistenceService persistence,
            Clock clock) {
        this.sessions = sessions;
        this.sessionItems = sessionItems;
        this.addresses = addresses;
        this.voucherPricing = voucherPricing;
        this.payments = payments;
        this.pricing = pricing;
        this.packages = packages;
        this.persistence = persistence;
        this.clock = clock;
    }

    /** External calls deliberately happen outside a database transaction. */
    public CheckoutPreviewResponse preview(UUID customerId, UUID checkoutSessionId, CheckoutPreviewRequest request) {
        CheckoutSession session = sessions.findByIdAndCustomerId(checkoutSessionId, customerId)
                .orElseThrow(() -> new OrderException(HttpStatus.NOT_FOUND, "CHECKOUT_SESSION_NOT_FOUND",
                        "Không tìm thấy Checkout Session: " + checkoutSessionId));
        requirePreviewable(session);
        List<CheckoutSessionItem> items = sessionItems.findAllByCheckoutSessionId(checkoutSessionId);
        PackageMetrics packageMetrics = packages.calculate(items);

        AddressSnapshot address = addresses.loadOwnedAddress(customerId, session.getAddressId());
        validateAddress(session, address);
        VoucherPreview voucherResult = loadVoucherPreview(customerId, request, items);
        Map<UUID, LineDiscount> discounts = validateVoucherResult(request, voucherResult, items);

        long shippingDiscount = voucherResult.vouchers().stream()
                .mapToLong(AppliedVoucher::shippingDiscountVnd)
                .sum();
        PaymentClient.ShippingQuoteResponse quote = payments.createShippingQuote(new ShippingQuoteRequest(
                customerId,
                address.provinceId(),
                address.wardId(),
                items.stream().map(this::toShippingItem).toList(),
                shippingDiscount,
                normalizeServiceCode(request.serviceCode())));
        validateQuote(quote, shippingDiscount);

        PricingBreakdown breakdown = pricing.calculate(new PricingRequest(
                items.stream().map(item -> toLinePricing(item, discounts.get(item.getVariantId()))).toList(),
                quote.feeVnd(),
                quote.shippingDiscountVnd()));
        PersistedPreview stored = persistence.persist(new PreviewCommit(
                customerId, checkoutSessionId, session.getSelectionFingerprint(), address,
                voucherResult.vouchers(), breakdown, quote, packageMetrics));
        return toResponse(session, address, voucherResult.vouchers(), quote, breakdown, stored);
    }

    private void requirePreviewable(CheckoutSession session) {
        if (session.getStatus() != CheckoutStatus.ACTIVE) {
            throw new OrderException(HttpStatus.CONFLICT, "CHECKOUT_SESSION_NOT_ACTIVE",
                    "Checkout Session không còn ở trạng thái ACTIVE.");
        }
        if (!session.getExpiresAt().isAfter(Instant.now(clock))) {
            throw new OrderException(HttpStatus.CONFLICT, "CHECKOUT_SESSION_EXPIRED", "Checkout Session đã hết hạn.");
        }
        if (session.getAddressId() == null) {
            throw new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "CHECKOUT_ADDRESS_REQUIRED",
                    "Cần chọn địa chỉ trước khi Preview.");
        }
    }

    private void validateAddress(CheckoutSession session, AddressSnapshot address) {
        if (address == null || address.addressId() == null || !address.addressId().equals(session.getAddressId())
                || address.provinceId() <= 0 || address.wardId() <= 0
                || isBlank(address.recipientName()) || isBlank(address.phone()) || isBlank(address.addressLine())
                || isBlank(address.provinceName()) || isBlank(address.wardName())) {
            throw new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_ADDRESS_SNAPSHOT",
                    "Identity Service trả về địa chỉ không hợp lệ hoặc không đúng lựa chọn.");
        }
    }

    private VoucherPreview loadVoucherPreview(
            UUID customerId, CheckoutPreviewRequest request, List<CheckoutSessionItem> items) {
        if (request.merchandiseVoucherId() == null && request.shippingVoucherId() == null) {
            return VoucherPreview.empty();
        }
        return voucherPricing.preview(new VoucherPreviewRequest(
                customerId,
                request.merchandiseVoucherId(),
                request.shippingVoucherId(),
                items.stream().map(item -> new VoucherItem(
                        item.getProductId(), item.getVariantId(), item.getQuantity(), item.getUnitPriceVnd())).toList()));
    }

    private Map<UUID, LineDiscount> validateVoucherResult(
            CheckoutPreviewRequest request, VoucherPreview result, List<CheckoutSessionItem> items) {
        if (result == null) {
            throw invalidVoucher("Cart/Voucher không trả kết quả Preview.");
        }
        Set<UUID> expectedVoucherIds = new HashSet<>();
        if (request.merchandiseVoucherId() != null) {
            expectedVoucherIds.add(request.merchandiseVoucherId());
        }
        if (request.shippingVoucherId() != null) {
            expectedVoucherIds.add(request.shippingVoucherId());
        }
        Set<UUID> actualVoucherIds = new HashSet<>();
        long merchandiseDiscount = 0;
        int shippingCount = 0;
        int merchandiseCount = 0;
        for (AppliedVoucher voucher : result.vouchers()) {
            if (voucher == null || voucher.voucherId() == null || !actualVoucherIds.add(voucher.voucherId())
                    || isBlank(voucher.voucherCode()) || isBlank(voucher.scope())
                    || voucher.discountAmountVnd() < 0 || voucher.shippingDiscountVnd() < 0) {
                throw invalidVoucher("Voucher Preview chứa snapshot không hợp lệ hoặc bị trùng.");
            }
            if (SHIPPING_SCOPE.equals(voucher.scope())) {
                shippingCount++;
                if (voucher.discountAmountVnd() != 0) {
                    throw invalidVoucher("Voucher giao hàng không được giảm trực tiếp tiền sản phẩm.");
                }
            } else {
                merchandiseCount++;
                if (voucher.shippingDiscountVnd() != 0) {
                    throw invalidVoucher("Voucher hàng hóa không được giảm phí giao hàng.");
                }
                merchandiseDiscount = addMoney(merchandiseDiscount, voucher.discountAmountVnd());
            }
        }
        if (!actualVoucherIds.equals(expectedVoucherIds) || shippingCount > 1 || merchandiseCount > 1) {
            throw invalidVoucher("Voucher trả về không khớp lựa chọn hoặc vi phạm stacking rule P0.");
        }

        Set<UUID> knownVariants = items.stream().map(CheckoutSessionItem::getVariantId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, LineDiscount> discounts = new HashMap<>();
        long allocated = 0;
        for (LineDiscount line : result.lineDiscounts()) {
            if (line == null || line.variantId() == null || !knownVariants.contains(line.variantId())
                    || discounts.putIfAbsent(line.variantId(), line) != null
                    || line.productDiscountVnd() < 0 || line.orderDiscountVnd() < 0) {
                throw invalidVoucher("Phân bổ giảm giá theo dòng không hợp lệ.");
            }
            allocated = addMoney(allocated, addMoney(line.productDiscountVnd(), line.orderDiscountVnd()));
        }
        if (allocated != merchandiseDiscount) {
            throw invalidVoucher("Tổng phân bổ giảm giá theo dòng không khớp voucher hàng hóa.");
        }
        return discounts;
    }

    private void validateQuote(PaymentClient.ShippingQuoteResponse quote, long requestedShippingDiscount) {
        Instant now = Instant.now(clock);
        if (quote == null || quote.quoteId() == null || quote.feeVnd() < 0
                || quote.shippingDiscountVnd() != requestedShippingDiscount
                || quote.shippingDiscountVnd() < 0 || quote.shippingDiscountVnd() > quote.feeVnd()
                || quote.payableFeeVnd() != quote.feeVnd() - quote.shippingDiscountVnd()
                || quote.serviceId() <= 0 || isBlank(quote.serviceName())
                || quote.expiresAt() == null || !quote.expiresAt().isAfter(now)
                || quote.requestFingerprint() == null
                || !quote.requestFingerprint().matches("[0-9a-fA-F]{64}")) {
            throw new OrderException(HttpStatus.BAD_GATEWAY, "INVALID_SHIPPING_QUOTE_CONTRACT",
                    "Payment Service trả về báo giá thiếu hoặc không hợp lệ.");
        }
    }

    private ShippingItemRequest toShippingItem(CheckoutSessionItem item) {
        return new ShippingItemRequest(
                item.getVariantId(), item.getQuantity(), item.getWeightGrams(),
                item.getLengthCm(), item.getWidthCm(), item.getHeightCm());
    }

    private LinePricing toLinePricing(CheckoutSessionItem item, LineDiscount discount) {
        long productDiscount = discount == null ? 0 : discount.productDiscountVnd();
        long orderDiscount = discount == null ? 0 : discount.orderDiscountVnd();
        return new LinePricing(item.getVariantId(), item.getListPriceVnd(), item.getDirectSaleDiscountVnd(),
                item.getQuantity(), productDiscount, orderDiscount);
    }

    private CheckoutPreviewResponse toResponse(
            CheckoutSession session,
            AddressSnapshot address,
            List<AppliedVoucher> vouchers,
            PaymentClient.ShippingQuoteResponse quote,
            PricingBreakdown money,
            PersistedPreview stored) {
        return new CheckoutPreviewResponse(
                session.getId(),
                address.addressId(),
                vouchers.stream().map(voucher -> new VoucherResponse(
                        voucher.voucherId(), voucher.voucherCode(), voucher.scope(),
                        voucher.discountAmountVnd(), voucher.shippingDiscountVnd())).toList(),
                new ShippingQuoteResponse(
                        quote.quoteId(), quote.feeVnd(), quote.shippingDiscountVnd(), quote.payableFeeVnd(),
                        quote.serviceId(), quote.serviceName(), quote.eta(), quote.expiresAt()),
                new MoneyBreakdown(
                        money.itemsListSubtotalVnd(), money.directSaleDiscountVnd(), money.itemsSubtotalVnd(),
                        money.productDiscountVnd(), money.orderDiscountVnd(), money.shippingFeeVnd(),
                        money.shippingDiscountVnd(), money.finalTotalVnd()),
                stored.paymentTiming(),
                stored.paymentMethod());
    }

    private long addMoney(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw invalidVoucher("Giá trị giảm giá vượt giới hạn hỗ trợ.");
        }
    }

    private String normalizeServiceCode(String serviceCode) {
        return isBlank(serviceCode) ? null : serviceCode.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private OrderException invalidVoucher(String message) {
        return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VOUCHER_PREVIEW", message);
    }
}
