package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.AddressGateway.AddressSnapshot;
import com.dynamicmart.order_service.client.PaymentClient.ShippingQuoteResponse;
import com.dynamicmart.order_service.client.VoucherPricingGateway.AppliedVoucher;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSessionVoucher;
import com.dynamicmart.order_service.entity.CheckoutShippingQuote;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingBreakdown;
import com.dynamicmart.order_service.service.ShippingPackageCalculator.PackageMetrics;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutPreviewPersistenceService {
    private final CheckoutSessionRepository sessions;
    private final CheckoutSessionVoucherRepository vouchers;
    private final CheckoutShippingQuoteRepository quotes;
    private final Clock clock;

    public CheckoutPreviewPersistenceService(
            CheckoutSessionRepository sessions,
            CheckoutSessionVoucherRepository vouchers,
            CheckoutShippingQuoteRepository quotes,
            Clock clock) {
        this.sessions = sessions;
        this.vouchers = vouchers;
        this.quotes = quotes;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = OrderException.class)
    public PersistedPreview persist(PreviewCommit commit) {
        Instant now = Instant.now(clock);
        CheckoutSession session = sessions.findOwnedForUpdate(commit.checkoutSessionId(), commit.customerId())
                .orElseThrow(() -> new OrderException(HttpStatus.NOT_FOUND, "CHECKOUT_SESSION_NOT_FOUND",
                        "Không tìm thấy Checkout Session: " + commit.checkoutSessionId()));
        requireStillCurrent(session, commit, now);

        CheckoutShippingQuote existingQuote = quotes.findByProviderAndQuoteId("GHN", commit.quote().quoteId())
                .orElse(null);
        if (existingQuote != null && !existingQuote.getCheckoutSessionId().equals(session.getId())) {
            throw new OrderException(HttpStatus.CONFLICT, "SHIPPING_QUOTE_ALREADY_LINKED",
                    "Báo giá giao hàng đã thuộc một Checkout Session khác.");
        }

        quotes.findAllByCheckoutSessionIdAndStatus(session.getId(), ShippingQuoteStatus.ACTIVE)
                .forEach(quote -> quote.invalidate(now));
        CheckoutShippingQuote storedQuote = existingQuote == null
                ? createQuote(commit, now)
                : refreshQuote(existingQuote, commit, now);
        quotes.save(storedQuote);

        vouchers.deleteAllByCheckoutSessionId(session.getId());
        List<CheckoutSessionVoucher> storedVouchers = commit.appliedVouchers().stream()
                .map(voucher -> CheckoutSessionVoucher.create(
                        UUID.randomUUID(), session.getId(), voucher.voucherId(), voucher.voucherCode(), voucher.scope(),
                        voucher.discountAmountVnd(), voucher.shippingDiscountVnd(), now))
                .toList();
        vouchers.saveAll(storedVouchers);

        applyMoney(session, commit.pricing());
        if (commit.pricing().finalTotalVnd() == 0) {
            session.setPaymentTiming(PaymentTiming.NOT_REQUIRED);
            session.setPaymentMethod(PaymentMethod.FREE);
        } else if (session.getPaymentTiming() == PaymentTiming.NOT_REQUIRED || session.getPaymentMethod() == PaymentMethod.FREE) {
            session.setPaymentTiming(null);
            session.setPaymentMethod(null);
        }
        session.setUpdatedAt(now);
        return new PersistedPreview(session.getPaymentTiming(), session.getPaymentMethod());
    }

    private void requireStillCurrent(CheckoutSession session, PreviewCommit commit, Instant now) {
        if (session.getStatus() == CheckoutStatus.ACTIVE && !session.getExpiresAt().isAfter(now)) {
            session.setStatus(CheckoutStatus.EXPIRED);
            session.setUpdatedAt(now);
            throw new OrderException(HttpStatus.CONFLICT, "CHECKOUT_SESSION_EXPIRED", "Checkout Session đã hết hạn.");
        }
        if (session.getStatus() != CheckoutStatus.ACTIVE) {
            throw new OrderException(HttpStatus.CONFLICT, "CHECKOUT_SESSION_NOT_ACTIVE",
                    "Checkout Session không còn ở trạng thái ACTIVE.");
        }
        if (!session.getSelectionFingerprint().equals(commit.selectionFingerprint())
                || !commit.address().addressId().equals(session.getAddressId())) {
            throw new OrderException(HttpStatus.CONFLICT, "CHECKOUT_PREVIEW_STALE",
                    "Checkout đã thay đổi trong khi báo giá. Vui lòng Preview lại.");
        }
    }

    private CheckoutShippingQuote createQuote(PreviewCommit commit, Instant now) {
        return CheckoutShippingQuote.create(
                UUID.randomUUID(), commit.checkoutSessionId(), commit.quote().quoteId(), commit.quote().requestFingerprint(),
                commit.quote().feeVnd(), commit.quote().shippingDiscountVnd(), commit.quote().payableFeeVnd(),
                commit.quote().serviceId(), commit.quote().serviceName(), commit.quote().eta(),
                commit.packageMetrics().totalWeightGrams(), commit.packageMetrics().lengthCm(),
                commit.packageMetrics().widthCm(), commit.packageMetrics().heightCm(),
                commit.address().provinceId(), commit.address().wardId(), commit.address().provinceName(),
                commit.address().wardName(), commit.quote().expiresAt(), now);
    }

    private CheckoutShippingQuote refreshQuote(
            CheckoutShippingQuote quote, PreviewCommit commit, Instant now) {
        quote.refresh(
                commit.quote().requestFingerprint(), commit.quote().feeVnd(), commit.quote().shippingDiscountVnd(),
                commit.quote().payableFeeVnd(), commit.quote().serviceId(), commit.quote().serviceName(),
                commit.quote().eta(), commit.packageMetrics().totalWeightGrams(), commit.packageMetrics().lengthCm(),
                commit.packageMetrics().widthCm(), commit.packageMetrics().heightCm(), commit.address().provinceId(),
                commit.address().wardId(), commit.address().provinceName(), commit.address().wardName(),
                commit.quote().expiresAt(), now);
        return quote;
    }

    private void applyMoney(CheckoutSession session, PricingBreakdown pricing) {
        session.setItemsListSubtotalVnd(pricing.itemsListSubtotalVnd());
        session.setDirectSaleDiscountVnd(pricing.directSaleDiscountVnd());
        session.setItemsSubtotalVnd(pricing.itemsSubtotalVnd());
        session.setProductDiscountVnd(pricing.productDiscountVnd());
        session.setOrderDiscountVnd(pricing.orderDiscountVnd());
        session.setShippingFeeVnd(pricing.shippingFeeVnd());
        session.setShippingDiscountVnd(pricing.shippingDiscountVnd());
        session.setFinalTotalVnd(pricing.finalTotalVnd());
    }

    public record PreviewCommit(
            UUID customerId,
            UUID checkoutSessionId,
            String selectionFingerprint,
            AddressSnapshot address,
            List<AppliedVoucher> appliedVouchers,
            PricingBreakdown pricing,
            ShippingQuoteResponse quote,
            PackageMetrics packageMetrics) {
        public PreviewCommit {
            appliedVouchers = List.copyOf(appliedVouchers);
        }
    }

    public record PersistedPreview(PaymentTiming paymentTiming, PaymentMethod paymentMethod) {
    }
}
