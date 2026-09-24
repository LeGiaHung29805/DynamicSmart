package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCreationContextReader {
    private final OrderSagaRepository sagas;
    private final CheckoutSessionRepository sessions;
    private final CheckoutSessionItemRepository items;
    private final CheckoutSessionVoucherRepository vouchers;
    private final CheckoutShippingQuoteRepository quotes;
    private final Clock clock;

    public OrderCreationContextReader(
            OrderSagaRepository sagas,
            CheckoutSessionRepository sessions,
            CheckoutSessionItemRepository items,
            CheckoutSessionVoucherRepository vouchers,
            CheckoutShippingQuoteRepository quotes,
            Clock clock) {
        this.sagas = sagas;
        this.sessions = sessions;
        this.items = items;
        this.vouchers = vouchers;
        this.quotes = quotes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CreationContext load(UUID customerId, UUID sagaId) {
        OrderSaga saga = sagas.findById(sagaId)
                .orElseThrow(() -> notFound("ORDER_SAGA_NOT_FOUND", "Không tìm thấy Create Order Saga."));
        CheckoutSession session = sessions.findByIdAndCustomerId(saga.getCheckoutSessionId(), customerId)
                .orElseThrow(() -> notFound("CHECKOUT_SESSION_NOT_FOUND", "Không tìm thấy Checkout Session của khách hàng."));
        Instant now = Instant.now(clock);
        if (saga.getStatus() != SagaStatus.STARTED) {
            throw conflict("ORDER_SAGA_NOT_REVALIDATABLE", "Create Order Saga không còn ở bước revalidation.");
        }
        if (session.getStatus() != CheckoutStatus.ACTIVE || !session.getExpiresAt().isAfter(now)) {
            throw conflict("CHECKOUT_SESSION_NOT_ACTIVE", "Checkout Session không còn hiệu lực để tạo Order.");
        }
        if (session.getAddressId() == null || session.getPaymentTiming() == null || session.getPaymentMethod() == null
                || session.getItemsListSubtotalVnd() == null || session.getDirectSaleDiscountVnd() == null
                || session.getItemsSubtotalVnd() == null || session.getProductDiscountVnd() == null
                || session.getOrderDiscountVnd() == null || session.getShippingFeeVnd() == null
                || session.getShippingDiscountVnd() == null || session.getFinalTotalVnd() == null) {
            throw conflict("CHECKOUT_PREVIEW_REQUIRED", "Checkout không còn snapshot Preview đầy đủ.");
        }

        var activeQuote = quotes.findFirstByCheckoutSessionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        session.getId(), ShippingQuoteStatus.ACTIVE, now)
                .orElseThrow(() -> conflict("ACTIVE_SHIPPING_QUOTE_REQUIRED",
                        "Checkout cần Preview lại để có báo giá giao hàng còn hiệu lực."));
        List<ItemSnapshot> itemSnapshots = items.findAllByCheckoutSessionId(session.getId()).stream()
                .map(item -> new ItemSnapshot(
                        item.getSourceCartItemId(), item.getSourceCartItemVersion(), item.getProductId(), item.getVariantId(),
                        item.getSku(), item.getProductName(), item.getVariantName(), item.getImageUrl(),
                        item.getListPriceVnd(), item.getDirectSalePromotionId(), item.getDirectSaleDiscountVnd(),
                        item.getUnitPriceVnd(), item.getQuantity(), item.getWeightGrams(), item.getLengthCm(),
                        item.getWidthCm(), item.getHeightCm()))
                .toList();
        if (itemSnapshots.isEmpty()) {
            throw conflict("CHECKOUT_ITEMS_REQUIRED", "Checkout Session không còn snapshot sản phẩm.");
        }
        List<VoucherSnapshot> voucherSnapshots = vouchers.findAllByCheckoutSessionId(session.getId()).stream()
                .map(voucher -> new VoucherSnapshot(
                        voucher.getVoucherId(), voucher.getVoucherCode(), voucher.getScope(),
                        voucher.getDiscountAmountVnd(), voucher.getShippingDiscountVnd()))
                .toList();

        return new CreationContext(
                saga.getId(), saga.getCorrelationId(), session.getId(), session.getCustomerId(), session.getSource(),
                session.getCartId(), session.getAddressId(), session.getPaymentTiming(), session.getPaymentMethod(),
                new MoneySnapshot(
                        session.getItemsListSubtotalVnd(), session.getDirectSaleDiscountVnd(), session.getItemsSubtotalVnd(),
                        session.getProductDiscountVnd(), session.getOrderDiscountVnd(), session.getShippingFeeVnd(),
                        session.getShippingDiscountVnd(), session.getFinalTotalVnd()),
                itemSnapshots, voucherSnapshots,
                new QuoteSnapshot(
                        activeQuote.getId(), activeQuote.getQuoteId(), activeQuote.getInputFingerprint(),
                        activeQuote.getFeeVnd(), activeQuote.getShippingDiscountVnd(), activeQuote.getPayableFeeVnd(),
                        activeQuote.getServiceId(), activeQuote.getServiceName(), activeQuote.getEtaText(),
                        activeQuote.getTotalWeightGrams(), activeQuote.getPackageLengthCm(),
                        activeQuote.getPackageWidthCm(), activeQuote.getPackageHeightCm(),
                        activeQuote.getToProvinceId(), activeQuote.getToWardId(),
                        activeQuote.getToProvinceName(), activeQuote.getToWardName(), activeQuote.getExpiresAt()));
    }

    private OrderException notFound(String code, String message) {
        return new OrderException(HttpStatus.NOT_FOUND, code, message);
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    public record CreationContext(
            UUID sagaId,
            UUID correlationId,
            UUID checkoutSessionId,
            UUID customerId,
            CheckoutSource source,
            UUID cartId,
            UUID addressId,
            PaymentTiming paymentTiming,
            PaymentMethod paymentMethod,
            MoneySnapshot money,
            List<ItemSnapshot> items,
            List<VoucherSnapshot> vouchers,
            QuoteSnapshot quote) {
        public CreationContext {
            items = List.copyOf(items);
            vouchers = List.copyOf(vouchers);
        }
    }

    public record MoneySnapshot(
            long itemsListSubtotalVnd,
            long directSaleDiscountVnd,
            long itemsSubtotalVnd,
            long productDiscountVnd,
            long orderDiscountVnd,
            long shippingFeeVnd,
            long shippingDiscountVnd,
            long finalTotalVnd) {
    }

    public record ItemSnapshot(
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
            long unitPriceVnd,
            int quantity,
            int weightGrams,
            Integer lengthCm,
            Integer widthCm,
            Integer heightCm) {
    }

    public record VoucherSnapshot(
            UUID voucherId,
            String voucherCode,
            String scope,
            long discountAmountVnd,
            long shippingDiscountVnd) {
    }

    public record QuoteSnapshot(
            UUID checkoutQuoteId,
            UUID providerQuoteId,
            String inputFingerprint,
            long feeVnd,
            long shippingDiscountVnd,
            long payableFeeVnd,
            int serviceId,
            String serviceName,
            String eta,
            Integer totalWeightGrams,
            Integer packageLengthCm,
            Integer packageWidthCm,
            Integer packageHeightCm,
            Integer provinceId,
            Integer wardId,
            String provinceName,
            String wardName,
            Instant expiresAt) {
    }
}
